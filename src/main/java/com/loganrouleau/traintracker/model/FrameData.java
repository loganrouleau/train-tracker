package com.loganrouleau.traintracker.model;

import javafx.scene.image.Image;

/**
 * A collection of metrics collected every frame by the model. This is passed to a controller to be used as desired.
 */
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