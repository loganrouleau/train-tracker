package com.loganrouleau.traintracker;

import javafx.scene.image.Image;

public class FrameData {
    private Image image;
    private boolean trainDetected;
    private double diffFrameIntensitySum;

    public FrameData(Image image) {
        this.image = image;
    }

    public FrameData(Image image, boolean trainDetected, double diffFrameIntensitySum) {
        this.image = image;
        this.trainDetected = trainDetected;
        this.diffFrameIntensitySum = diffFrameIntensitySum;
    }

    public Image getImage() {
        return image;
    }

    public boolean isTrainDetected() {
        return trainDetected;
    }

    public double getDiffFrameIntensitySum() {
        return diffFrameIntensitySum;
    }
}