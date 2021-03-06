package com.loganrouleau.traintracker;

import com.loganrouleau.traintracker.controller.BaseController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;

/**
 * The JavaFX application main class. It creates and handles the display and its resources.
 */
public class Main extends Application {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    /**
     * The main entry point for the application, called on the JavaFX Application Thread after the
     * {@link Application#init()} method returns.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/loganrouleau/traintracker/view/base.fxml"));
            // store the root element so that the controllers can use it
            BorderPane rootElement = loader.load();

            Scene scene = new Scene(rootElement);

            primaryStage.setTitle("Train Tracker");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // set the proper behavior on closing the application
            BaseController controller = loader.getController();
            primaryStage.setOnCloseRequest((windowEvent -> controller.onWindowCloseRequest()));
        } catch (Exception e) {
            LOG.error("Error starting application: " + e);
        }
    }

    public static void main(String[] args) {
        LOG.info("Entering main");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Config.loadProperties();
        launch(args);
        LOG.info("Exiting main");
    }
}