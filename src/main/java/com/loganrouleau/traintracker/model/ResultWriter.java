package com.loganrouleau.traintracker.model;

import com.loganrouleau.traintracker.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ResultWriter {
    private static ResultWriter resultWriter = new ResultWriter();
    private static FileWriter fileWriter;
    private static final String RESULT_LINE_FORMAT = "%n%s,%s,%.0f,%.0f,%s";

    static {
        try {
            fileWriter = new FileWriter("C:\\Users\\lroul\\projects\\train-tracker\\results\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(Config.TIMESTAMP_FORMAT)) + ".csv");
            fileWriter.write(String.format("%s,%s,%s,%s,%s", "Timestamp", "Location", "Threshold Slider Value",
                    "Detection Tolerance Slider Value", "Direction"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ResultWriter() {
    }

    public static ResultWriter getInstance() {
        return resultWriter;
    }

    public void writeResultLine(String timestamp, String location, double thresholdValue, double detectionValue,
                                String direction) {
        try {
            fileWriter.write(String.format(RESULT_LINE_FORMAT, timestamp, location, thresholdValue, detectionValue,
                    direction));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
