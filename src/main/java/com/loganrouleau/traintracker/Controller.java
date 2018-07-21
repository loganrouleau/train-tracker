package com.loganrouleau.traintracker;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 */
public class Controller implements Observer {
    private static final Logger LOG = LogManager.getLogger(Controller.class);

    @FXML
    private Button captureButton;
    @FXML
    private Button calibrateButton;
    @FXML
    private Slider thresholdSlider;
    @FXML
    private Slider detectionToleranceSlider;
    @FXML
    private ImageView imageView;
    @FXML
    private Label statusLabel;
    @FXML
    private Label motionLabel;
    @FXML
    private Label label;
    @FXML
    private TextField x1Text;
    @FXML
    private TextField y1Text;
    @FXML
    private TextField x2Text;
    @FXML
    private TextField y2Text;

    private MotionDetector motionDetector;
    private MediaPlayer mediaPlayer;

    @FXML
    public void initialize() {
        motionDetector = new MotionDetector();
        motionDetector.addObserver(this);
        Media sound = new Media(Paths.get("C:\\Users\\lroul\\projects\\train-tracker\\src\\main\\resources\\camera-click.wav").toUri().toString());
        mediaPlayer = new MediaPlayer(sound);
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
        // TODO: duplicate method call
        motionDetector.setBoundingBox(Integer.parseInt(x1Text.getText()), Integer.parseInt(y1Text.getText()),
                Integer.parseInt(x2Text.getText()), Integer.parseInt(y2Text.getText()));
        motionDetector.setThreshold(thresholdSlider.getValue());
        motionDetector.setDetectionTolerance(detectionToleranceSlider.getValue());

        // If already active, stop the capture
        if (motionDetector.isCameraActive()) {
            motionDetector.setCameraActive(false);
            captureButton.setText("Start Camera");
            motionDetector.stopAcquisition();
            return;
        }

        motionDetector.capture();
        motionDetector.setCameraActive(true);
        captureButton.setText("Stop Camera");
    }

    @FXML
    public void onCalibrate() {
        motionDetector.setCalibrating(!motionDetector.isCalibrating());
        calibrateButton.setText(motionDetector.isCalibrating() ? "Stop" : "Calibrate");
    }

    private void updateImage() {
        motionDetector.setBoundingBox(Integer.parseInt(x1Text.getText()), Integer.parseInt(y1Text.getText()),
                Integer.parseInt(x2Text.getText()), Integer.parseInt(y2Text.getText()));

        if (motionDetector.isCameraActive()) {
            return;
        }

        motionDetector.updateImageNew();
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    public void onWindowCloseRequest() {
        mediaPlayer.stop();
        motionDetector.stopAcquisition();
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            FrameData frameData = (FrameData) arg;

            Platform.runLater(() -> imageView.imageProperty().set(frameData.getImage()));
            Platform.runLater(() -> motionLabel.setText("Diff sum: " + frameData.getDiffFrameIntensitySum()));

            if (frameData.isTrainDetected()) {
                Platform.runLater(() -> statusLabel.setText("Train detected!"));
                mediaPlayer.stop();
                mediaPlayer.play();
            } else {
                Platform.runLater(() -> statusLabel.setText(""));
            }
        } catch (Exception e) {
            LOG.warn("Expected instance of FrameData. Skipping update");
        }
    }
}
