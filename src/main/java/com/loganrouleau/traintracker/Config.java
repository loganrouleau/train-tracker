package com.loganrouleau.traintracker;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    public static double CAPTURE_DURATION_MINUTES;
    public static int FRAMES_PER_SECOND;
    public static String IMAGE_OUTPUT_DIRECTORY;
    public static String IMAGE_EXTENSION;
    public static String TIMESTAMP_FORMAT;
    public static int CAMERA_ID;

    public static void loadProperties() {
        Properties properties = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream("train-tracker.properties");
            properties.load(in);
            in.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        CAPTURE_DURATION_MINUTES = Double.parseDouble(properties.getProperty("capture.duration.minutes"));
        FRAMES_PER_SECOND = Integer.parseInt(properties.getProperty("frames.per.second"));
        IMAGE_OUTPUT_DIRECTORY = properties.getProperty("image.output.directory");
        IMAGE_EXTENSION = properties.getProperty("image.extension");
        TIMESTAMP_FORMAT = properties.getProperty("timestamp.format");
        CAMERA_ID = Integer.parseInt(properties.getProperty("camera.id"));
    }
}
