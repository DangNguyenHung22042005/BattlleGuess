package com.battleguess.battleguess.canvas.model;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import java.util.ArrayList;

public class FillRegion {
    private ArrayList<Point2D> filledPixels;
    private transient Color color;

    public FillRegion(ArrayList<Point2D> filledPixels, Color color) {
        this.filledPixels = filledPixels;
        this.color = color;
    }

    public ArrayList<Point2D> getFilledPixels() {
        return filledPixels;
    }

    public Color getColor() {
        return color;
    }
}
