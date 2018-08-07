package com.loganrouleau.traintracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);

    // TODO: This config is unused
    public static double CAPTURE_DURATION_MINUTES;
    public static int FRAMES_PER_SECOND;
    public static String IMAGE_OUTPUT_DIRECTORY;
    public static String IMAGE_EXTENSION;
    public static String TIMESTAMP_FORMAT;
    public static int CAMERA_ID;
    public static boolean AUDIO_ENABLED;
    public static int DISPLAY_WIDTH_PIXELS;
    public static int DISPLAY_HEIGHT_PIXELS;

    public static void loadProperties() {
        Properties properties = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream("train-tracker.properties");
            properties.load(in);
            in.close();
        } catch (java.io.IOException e) {
            LOG.error("Unable to read in properties: " + e);
        }

        CAPTURE_DURATION_MINUTES = Double.parseDouble(properties.getProperty("capture.duration.minutes"));
        FRAMES_PER_SECOND = Integer.parseInt(properties.getProperty("frames.per.second"));
        IMAGE_OUTPUT_DIRECTORY = properties.getProperty("image.output.directory");
        IMAGE_EXTENSION = properties.getProperty("image.extension");
        TIMESTAMP_FORMAT = properties.getProperty("timestamp.format");
        CAMERA_ID = Integer.parseInt(properties.getProperty("camera.id"));
        AUDIO_ENABLED = Boolean.parseBoolean(properties.getProperty("audio.enabled"));
        DISPLAY_WIDTH_PIXELS = Integer.parseInt(properties.getProperty("display.width.pixels"));
        DISPLAY_HEIGHT_PIXELS = Integer.parseInt(properties.getProperty("display.height.pixels"));
    }
}
