package com.loganrouleau.traintracker;

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

public class MotionDetector extends Observable {
    private static final Logger LOG = LogManager.getLogger(MotionDetector.class);

    private VideoCapture capture;
    private boolean cameraActive = false;
    private boolean calibrating = false;

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

    public MotionDetector() {
        capture = new VideoCapture();
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);
    }

    public void setCameraActive(boolean cameraActive) {
        this.cameraActive = cameraActive;
    }

    public boolean isCameraActive() {
        return cameraActive;
    }

    public void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public boolean isCalibrating() {
        return calibrating;
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

    public void capture() {
        capture.open(Config.CAMERA_ID);
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            LOG.warn("Capture interrupted: " + e);
        }

        Rect captureBox = new Rect(point1, point2);
        double xScaleFactor = 1280 / (double) captureBox.width;
        double yScaleFactor = 720 / (double) captureBox.height;
        LOG.info(xScaleFactor + "   " + yScaleFactor);
        double scaleFactor = Math.min(xScaleFactor, yScaleFactor);

        Runnable frameGrabber = () -> {
            Mat currFrame = new Mat();
            String fileName = null;

            if (capture.isOpened()) {
                try {
                    capture.read(currFrame);

                    String imageFile = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern(Config.TIMESTAMP_FORMAT));
                    fileName = Config.IMAGE_OUTPUT_DIRECTORY + imageFile + "." + Config.IMAGE_EXTENSION;
                } catch (Exception e) {
                    LOG.warn("Exception during the image elaboration: " + e);
                }
            }

            Imgproc.cvtColor(currFrame, currFrame, Imgproc.COLOR_BGR2GRAY);
            currFrame = currFrame.submat(captureBox);

            if (prevFrame == null || !prevFrame.size().equals(currFrame.size())) {
                prevFrame = currFrame;
                return;
            }

            Mat diffFrame = new Mat();
            Core.absdiff(currFrame, prevFrame, diffFrame);
            Imgproc.threshold(diffFrame, diffFrame, thresholdSliderValue, 255, Imgproc.THRESH_BINARY);

            Moments moments = Imgproc.moments(diffFrame);
            double diffFrameIntensitySum = moments.m00;
            Point centroid = new Point(moments.m10 / diffFrameIntensitySum, moments.m01 / diffFrameIntensitySum);

            if (diffFrameIntensitySum > detectionToleranceSliderValue) {
                trainDetected = true;
                if (!calibrating) {
                    trainDetectedFrames = Math.min(trainDetectedFrames + 1, 3);
                    if (!trackingCentroids && trainDetectedFrames == 3) {
                        trackingCentroids = true;
                    }
                }
            } else {
                trainDetected = false;
                if (!calibrating) {
                    trainDetectedFrames = Math.max(trainDetectedFrames - 1, 0);
                    if (trackingCentroids && trainDetectedFrames == 0) {
                        // calculate direction
                        int direction = 0;
                        for (int i = 1; i < centroidList.size(); i++) {
                            if (Double.isNaN(centroidList.get(i - 1)) || Double.isNaN(centroidList.get(i))) {
                                LOG.debug(fileName + ": skipping NaN calc");
                                continue;
                            }
                            if (centroidList.get(i) > centroidList.get(i - 1)) {
                                direction++;
                            } else {
                                direction--;
                            }
                        }
                        String dir = direction > 0 ? "East" : "West";
                        LOG.info(fileName + ": Train detected moving " + dir);
                        trackingCentroids = false;
                        centroidList = new ArrayList<>();
                    }
                }
            }

            if (!calibrating && trackingCentroids) {
                centroidList.add(centroid.x);
            }


            if (!calibrating && trainDetected) {
                Imgcodecs.imwrite(fileName, currFrame);
            }

            if (!calibrating) {
                LOG.debug(fileName + "," + String.valueOf(diffFrameIntensitySum) + ", " +
                        String.valueOf(trainDetected) + ", " + String.valueOf(trainDetectedFrames) + ", " +
                        String.valueOf(centroid.x) + "," + String.valueOf(centroid.y) + ", " +
                        String.valueOf(trackingCentroids));
            }

            Core.bitwise_not(diffFrame, diffFrame);

            Mat displayFrame = new Mat();
            Imgproc.cvtColor(diffFrame, displayFrame, COLOR_GRAY2BGR);
            Imgproc.resize(displayFrame, displayFrame, new Size(scaleFactor * displayFrame.width(), scaleFactor * displayFrame.height()));
            centroid.set(new double[]{scaleFactor * centroid.x, scaleFactor * centroid.y});
            Imgproc.circle(displayFrame, centroid, 3, new Scalar(0, 0, 255), 4);
            Image imageToShow = Utils.mat2Image(displayFrame);
            setChanged();
            notifyObservers(new FrameData(imageToShow, trainDetected, diffFrameIntensitySum));

            prevFrame = currFrame;
        };

        timer = Executors.newSingleThreadScheduledExecutor();
        long period = Long.divideUnsigned(1000, Config.FRAMES_PER_SECOND);
        timer.scheduleAtFixedRate(frameGrabber, 500, period, TimeUnit.MILLISECONDS);
    }

    public void updateImageNew() {
        capture.open(Config.CAMERA_ID);
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);

        Mat frame = new Mat();
        try {
            capture.read(frame);
        } catch (Exception e) {
            LOG.warn("Exception during the image elaboration: " + e);
        }

        if (true) {
            Imgproc.rectangle(frame, point1, point2, new Scalar(0, 255, 0), 5);
        }

        Image imageToShow = Utils.mat2Image(frame);
        setChanged();
        notifyObservers(new FrameData(imageToShow));
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    public void stopAcquisition() {
        if (timer != null && !timer.isShutdown()) {
            try {
                timer.shutdownNow();
            } catch (SecurityException e) {
                LOG.warn("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (capture.isOpened()) {
            capture.release();
        }
    }
}
