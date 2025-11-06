package com.battleguess.battleguess.canvas.model;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import javafx.geometry.Point2D;

public class Line {
    private ArrayList<Point2D> points;
    private Color color;
    private double strokeSize;

    public Line(Color color, double strokeSize) {
        this.points = new ArrayList<>();
        this.color = color;
        this.strokeSize = strokeSize;
    }

    public void addPoint(Point2D point) {
        points.add(point);
    }

    public ArrayList<Point2D> getPoints() {
        return points;
    }

    public Color getColor() {
        return color;
    }

    public double getStrokeSize() {
        return strokeSize;
    }
}