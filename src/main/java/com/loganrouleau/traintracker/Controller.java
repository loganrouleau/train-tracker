package com.loganrouleau.traintracker;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CV_CAP_PROP_FRAME_WIDTH;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 */
public class Controller {
    private static final Logger LOG = LogManager.getLogger(Controller.class);

    @FXML
    private Button captureButton;
    @FXML
    private ImageView imageView;
    @FXML
    private Label motionLabel;
    @FXML
    private Label label;
    @FXML
    private TextField x1Text;
    @FXML
    private TextField x2Text;
    @FXML
    private TextField y1Text;
    @FXML
    private TextField y2Text;

    private ScheduledExecutorService timer;
    private boolean cameraActive = false;
    private Mat prevFrame = null;
    private BufferedWriter bufferedWriter;
    private MediaPlayer mediaPlayer;
    private VideoCapture capture;

    @FXML
    public void initialize() {
        try {
            bufferedWriter = new BufferedWriter(new FileWriter("output.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Media sound = new Media(Paths.get("C:\\Users\\lroul\\projects\\train-tracker\\src\\main\\resources\\camera-click.wav").toUri().toString());
        mediaPlayer = new MediaPlayer(sound);
        capture = new VideoCapture();
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);

        updateImage();
    }

    @FXML
    public void onMouseClicked() {
        updateImage();
    }

    @FXML
    public void onMouseMoved(MouseEvent mouseEvent) {
        label.setText("x: " + mouseEvent.getX() + " y: " + mouseEvent.getY());
    }

    /**
     * The action triggered by pushing the button on the GUI
     */
    @FXML
    protected void onCaptureButton() {
        // If already active, stop the capture
        if (cameraActive) {
            cameraActive = false;
            captureButton.setText("Start Camera");
            stopAcquisition();
            return;
        }
        capture.open(Config.CAMERA_ID);
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int x1 = Integer.parseInt(x1Text.getText());
        int x2 = Integer.parseInt(x2Text.getText());
        int y1 = Integer.parseInt(y1Text.getText());
        int y2 = Integer.parseInt(y2Text.getText());
        Rect captureBox = new Rect(x1, y1, (x2 - x1), (y2 - y1));

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
                    System.err.println("Exception during the image elaboration: " + e);
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

            Imgcodecs.imwrite(fileName, currFrame);
            mediaPlayer.stop();
            mediaPlayer.play();

            Moments moments = Imgproc.moments(diffFrame);
            double diffFrameIntensitySum = moments.m00;
            Point centroid = new Point(moments.m10 / diffFrameIntensitySum, moments.m01 / diffFrameIntensitySum);

            Platform.runLater(() -> motionLabel.setText("Diff sum: " + diffFrameIntensitySum));
            Utils.writeLine(bufferedWriter, fileName, diffFrameIntensitySum, centroid.x, centroid.y);

            Core.bitwise_not(diffFrame, diffFrame);

            Mat displayFrame = new Mat();
            Imgproc.cvtColor(diffFrame, displayFrame, COLOR_GRAY2BGR);
            Imgproc.circle(displayFrame, centroid, 3, new Scalar(0, 0, 255), 4);
            Image imageToShow = Utils.mat2Image(displayFrame);
            updateImageView(imageView, imageToShow);

            prevFrame = currFrame;
        };

        timer = Executors.newSingleThreadScheduledExecutor();
        long period = Long.divideUnsigned(1000, Config.FRAMES_PER_SECOND);
        timer.scheduleAtFixedRate(frameGrabber, 500, period, TimeUnit.MILLISECONDS);

        cameraActive = true;
        captureButton.setText("Stop Camera");
    }

    private void updateImage() {
        if (cameraActive) {
            return;
        }
        capture.open(Config.CAMERA_ID);
        capture.set(CV_CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(CV_CAP_PROP_FRAME_HEIGHT, 720);

        Mat frame = new Mat();
        try {
            capture.read(frame);
        } catch (Exception e) {
            System.err.println("Exception during the image elaboration: " + e);
        }

        if (x1Text.getText().length() > 0) {
            int x1 = Integer.parseInt(x1Text.getText());
            int x2 = Integer.parseInt(x2Text.getText());
            int y1 = Integer.parseInt(y1Text.getText());
            int y2 = Integer.parseInt(y2Text.getText());
            Imgproc.rectangle(frame, new Point(x1, y1), new Point(x2, y2), new Scalar(0, 255, 0), 5);
        }

        Image imageToShow = Utils.mat2Image(frame);
        updateImageView(imageView, imageToShow);
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (timer != null && !timer.isShutdown()) {
            try {
                timer.shutdownNow();
            } catch (SecurityException e) {
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (capture.isOpened()) {
            capture.release();
        }
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    public void onWindowCloseRequest() {
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.stop();
        stopAcquisition();
    }

}
