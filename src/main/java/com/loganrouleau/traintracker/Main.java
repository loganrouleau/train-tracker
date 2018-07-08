package com.loganrouleau.traintracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.net.URL;

/**
 * The main class for a JavaFX application. It creates and handle the main
 * window with its resources (style, graphics, etc.).
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/train-tracker.fxml"));
            // store the root element so that the controllers can use it
            BorderPane rootElement = loader.load();

            Scene scene = new Scene(rootElement);

            primaryStage.setTitle("Train Tracker");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // set the proper behavior on closing the application
            Controller controller = loader.getController();
            primaryStage.setOnCloseRequest((windowEvent -> controller.onWindowCloseRequest()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Config.loadProperties();
        launch(args);
    }
}