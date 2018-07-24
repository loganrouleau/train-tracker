package com.loganrouleau.traintracker.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public class BaseController {
    @FXML
    private BorderPane leftCamera;
    @FXML
    private BorderPane rightCamera;
    @FXML
    private CameraController leftCameraController;
    @FXML
    private CameraController rightCameraController;

    public void onWindowCloseRequest() {
        leftCameraController.onWindowCloseRequest();
        rightCameraController.onWindowCloseRequest();
    }
}
