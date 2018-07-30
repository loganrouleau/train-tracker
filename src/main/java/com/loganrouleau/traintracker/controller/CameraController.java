package com.loganrouleau.traintracker.controller;

import com.loganrouleau.traintracker.Config;
import com.loganrouleau.traintracker.model.FrameData;
import com.loganrouleau.traintracker.model.MotionDetector;
import com.loganrouleau.traintracker.model.ResultWriter;
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
public class CameraController extends BaseController implements Observer {
    private static final Logger LOG = LogManager.getLogger(CameraController.class);

    private MotionDetector motionDetector;
    private MediaPlayer mediaPlayer;
    private boolean cameraActive = false;

    // TODO: Make calibrate button shared among both views
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

    //TODO: Is the empty initialize method required?
    @FXML
    public void initialize() {
    }

    public void init(String location) {
        motionDetector = new MotionDetector();
        motionDetector.addObserver(this);
        motionDetector.setLocation(location);
        Media sound = new Media(Paths.get("C:\\Users\\lroul\\projects\\train-tracker\\src\\main\\resources\\camera-click.wav").toUri().toString());
        mediaPlayer = new MediaPlayer(sound);
        x1Text.setText(String.valueOf(0));
        y1Text.setText(String.valueOf(0));
        x2Text.setText(String.valueOf(Config.DISPLAY_WIDTH_PIXELS));
        y2Text.setText(String.valueOf(Config.DISPLAY_HEIGHT_PIXELS));
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

    @FXML
    public void onCalibrate() {
        motionDetector.setCalibrating(!motionDetector.isCalibrating());
        calibrateButton.setText(motionDetector.isCalibrating() ? "Stop" : "Calibrate");
    }

    /**
     * The action triggered by pushing the button on the GUI
     */
    @FXML
    protected void onCaptureButton() {
        if (cameraActive) {
            // If already active, stop the capture
            cameraActive = false;
            captureButton.setText("Start Camera");
            motionDetector.stopAcquisition(false);
        } else {
            // Start capture
            setBoundingBox();
            // TODO: Make these adjustments update view live
            motionDetector.setThreshold(thresholdSlider.getValue());
            motionDetector.setDetectionTolerance(detectionToleranceSlider.getValue());
            cameraActive = true;
            captureButton.setText("Stop Camera");
            motionDetector.capture();
        }
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    public void onWindowCloseRequest() {
        mediaPlayer.stop();
        motionDetector.stopAcquisition(true);
        ResultWriter.getInstance().close();
    }

    private void updateImage() {
        if (!cameraActive) {
            setBoundingBox();
            motionDetector.showPreviewImage();
        }
    }

    private void setBoundingBox() {
        motionDetector.setBoundingBox(Integer.parseInt(x1Text.getText()), Integer.parseInt(y1Text.getText()),
                Integer.parseInt(x2Text.getText()), Integer.parseInt(y2Text.getText()));
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            FrameData frameData = (FrameData) arg;

            Platform.runLater(() -> imageView.imageProperty().set(frameData.getImage()));
            Platform.runLater(() -> motionLabel.setText("Diff sum: " + frameData.getDiffFrameIntensitySum()));

            if (frameData.isTrainDetected()) {
                Platform.runLater(() -> statusLabel.setText("Train detected!"));
                if (Config.AUDIO_ENABLED) {
                    mediaPlayer.stop();
                    mediaPlayer.play();
                }
            } else {
                Platform.runLater(() -> statusLabel.setText(""));
            }
        } catch (Exception e) {
            LOG.warn("Expected instance of FrameData. Skipping update");
        }
    }
}
