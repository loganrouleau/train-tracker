package com.loganrouleau.traintracker.model;

import com.loganrouleau.traintracker.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class ResultWriter {
    private static ResultWriter resultWriter = new ResultWriter();
    private static FileWriter fileWriter;

    static {
        try {
            fileWriter = new FileWriter("C:\\Users\\lroul\\projects\\train-tracker\\results\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(Config.TIMESTAMP_FORMAT)) + ".csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        resultWriter.writeResultLine(Arrays.asList("Timestamp", "Location", "Direction", "Threshold Slider Value",
                "Detection Tolerance Slider Value"));
    }

    private ResultWriter() {
    }

    public static ResultWriter getInstance() {
        return resultWriter;
    }

    public void writeResultLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(value);
            sb.append(",");
        }
        sb.replace(sb.length() - 1, sb.length(), "\n");
        try {
            fileWriter.write(sb.toString());
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
