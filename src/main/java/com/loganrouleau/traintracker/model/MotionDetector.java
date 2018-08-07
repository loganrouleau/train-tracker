package com.loganrouleau.traintracker.model;

import com.loganrouleau.traintracker.Config;
import com.loganrouleau.traintracker.Utils;
import com.loganrouleau.traintracker.controller.CameraController;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_WIDTH;

/**
 * Interacts with a camera device on request by a {@link CameraController}. The public methods are thread safe so that
 * multiple controllers can read frames from the same device.
 */
public class MotionDetector extends Observable {
    private static final Logger LOG = LogManager.getLogger(MotionDetector.class);

    private VideoCapture capture = new VideoCapture();
    private boolean calibrating = false;
    private static int activeCaptures = 0;
    private String location;

    private ScheduledExecutorService timer;
    private boolean trainDetected = false;
    private boolean trackingCentroids = false;
    private List<Double> centroidList = new ArrayList<>();
    private int trainDetectedFrames = 0;
    private Mat prevFrame = null;
    private Point point1;
    private Point point2;

    private double thresholdSliderValue;
    private double detectionToleranceSliderValue;

    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar RED = new Scalar(0, 0, 255);

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public void setBoundingBox(int x1, int y1, int x2, int y2) {
        point1 = new Point(x1, y1);
        point2 = new Point(x2, y2);
    }

    public void setThreshold(double threshold) {
        thresholdSliderValue = threshold;
    }

    public void setDetectionTolerance(double detectionTolerance) {
        detectionToleranceSliderValue = detectionTolerance;
    }

    /**
     * The main capture loop, which uses OpenCV to process frames. Motion is detected by applying a frame difference
     * threshold. The x-component of the image intensity centroid is compared with the previous frame and binned as
     * positive or negative for a rough direction estimate. Once motion is no longer detected, the mode of the per-frame
     * direction estimates is used as the overall direction for that motion event.
     */
    public synchronized void capture() {
        if (activeCaptures == 0) {
            capture.open(Config.CAMERA_ID);
            capture.set(CV_CAP_PROP_FRAME_WIDTH, Config.DISPLAY_WIDTH_PIXELS);
            capture.set(CV_CAP_PROP_FRAME_HEIGHT, Config.DISPLAY_HEIGHT_PIXELS);
        }

        Rect captureBox = new Rect(point1, point2);
        double xScaleFactor = Config.DISPLAY_WIDTH_PIXELS / (double) captureBox.width;
        double yScaleFactor = Config.DISPLAY_HEIGHT_PIXELS / (double) captureBox.height;
        LOG.info(xScaleFactor + ", " + yScaleFactor);
        double scaleFactor = Math.min(xScaleFactor, yScaleFactor);

        Runnable frameGrabber = () -> {
            Mat currFrame = new Mat();
            String timestamp = null;
            String fileName = null;

            if (capture.isOpened()) {
                try {
                    capture.read(currFrame);
                    timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern(Config.TIMESTAMP_FORMAT));
                    fileName = Config.IMAGE_OUTPUT_DIRECTORY + timestamp + "." + Config.IMAGE_EXTENSION;
                } catch (Exception e) {
                    LOG.warn("Exception while reading frame: " + e);
                }
            }

            Imgproc.cvtColor(currFrame, currFrame, Imgproc.COLOR_BGR2GRAY);
            currFrame = currFrame.submat(captureBox);

            if (prevFrame == null || !prevFrame.size().equals(currFrame.size())) {
                // This is the first frame of the current capture, and there is no previous frame to diff against
                prevFrame = currFrame;
                return;
            }

            Mat diffFrame = new Mat();
            Core.absdiff(currFrame, prevFrame, diffFrame);
            Imgproc.threshold(diffFrame, diffFrame, thresholdSliderValue, 255, Imgproc.THRESH_BINARY);

            Moments moments = Imgproc.moments(diffFrame);
            double diffFrameIntensitySum = moments.m00;
            Point centroid = new Point(moments.m10 / diffFrameIntensitySum, moments.m01 / diffFrameIntensitySum);

            Mat displayFrame = new Mat();
            Core.bitwise_not(diffFrame, displayFrame);
            Imgproc.cvtColor(displayFrame, displayFrame, COLOR_GRAY2BGR);
            Imgproc.resize(displayFrame, displayFrame, new Size(scaleFactor * displayFrame.width(),
                    scaleFactor * displayFrame.height()));

            Point scaledCentroid = centroid;
            scaledCentroid.set(new double[]{scaleFactor * scaledCentroid.x, scaleFactor * scaledCentroid.y});
            Imgproc.circle(displayFrame, scaledCentroid, 3, RED, 4);

            Image imageToShow = Utils.mat2Image(displayFrame);
            setChanged();
            notifyObservers(new FrameData(imageToShow, trainDetected, diffFrameIntensitySum));

            prevFrame = currFrame;

            if (calibrating) {
                return;
            }

            if (diffFrameIntensitySum > detectionToleranceSliderValue) {
                trainDetected = true;
                trainDetectedFrames = Math.min(trainDetectedFrames + 1, 3);
                if (!trackingCentroids && trainDetectedFrames == 3) {
                    trackingCentroids = true;
                }
            } else {
                trainDetected = false;
                trainDetectedFrames = Math.max(trainDetectedFrames - 1, 0);
                if (trackingCentroids && trainDetectedFrames == 0) {
                    // calculate direction
                    int direction = 0;
                    for (int i = 1; i < centroidList.size(); i++) {
                        if (Double.isNaN(centroidList.get(i - 1)) || Double.isNaN(centroidList.get(i))) {
                            LOG.debug(timestamp + ": skipping NaN calc");
                            continue;
                        }
                        if (centroidList.get(i) > centroidList.get(i - 1)) {
                            direction++;
                        } else {
                            direction--;
                        }
                    }
                    String dir = direction > 0 ? "East" : "West";
                    LOG.info(timestamp + ": Train detected moving " + dir);
                    ResultWriter.getInstance().writeResultLine(timestamp, location, thresholdSliderValue,
                            detectionToleranceSliderValue, dir);

                    trackingCentroids = false;
                    centroidList = new ArrayList<>();
                }
            }
            LOG.debug(String.format("%s, %.0f, %b, %d, %.1f, %.1f, %b", timestamp, diffFrameIntensitySum, trainDetected,
                    trainDetectedFrames, centroid.x, centroid.y, trackingCentroids));

            if (trainDetected) {
                Imgcodecs.imwrite(fileName, currFrame);
            }

            // TODO: May be able to skip centroid calculation on frames where we aren't tracking centroids
            if (trackingCentroids) {
                centroidList.add(centroid.x);
            }
        };

        timer = Executors.newSingleThreadScheduledExecutor();
        long period = Long.divideUnsigned(1000, Config.FRAMES_PER_SECOND);
        timer.scheduleAtFixedRate(frameGrabber, 500, period, TimeUnit.MILLISECONDS);
        activeCaptures++;
    }

    /**
     * Read a single frame.
     */
    public synchronized void showPreviewImage() {
        if (activeCaptures == 0) {
            capture.open(Config.CAMERA_ID);
            capture.set(CV_CAP_PROP_FRAME_WIDTH, Config.DISPLAY_WIDTH_PIXELS);
            capture.set(CV_CAP_PROP_FRAME_HEIGHT, Config.DISPLAY_HEIGHT_PIXELS);
        }

        Mat frame = new Mat();
        try {
            capture.read(frame);
        } catch (Exception e) {
            LOG.warn("Exception while reading frame: " + e);
        }

        Imgproc.rectangle(frame, point1, point2, GREEN, 5);
        Image imageToShow = Utils.mat2Image(frame);
        setChanged();
        notifyObservers(new FrameData(imageToShow));
    }

    /**
     * Stop the acquisition from the camera and release all the resources.
     */
    public synchronized void stopAcquisition(boolean forceStop) {
        activeCaptures--;
        if ((capture.isOpened() && activeCaptures == 0) || forceStop) {
            capture.release();
        }

        if (timer != null && !timer.isShutdown()) {
            try {
                timer.shutdownNow();
            } catch (SecurityException e) {
                LOG.warn("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }
    }
}