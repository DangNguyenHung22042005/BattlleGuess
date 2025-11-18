package com.battleguess.battleguess.canvas;

import com.battleguess.battleguess.canvas.model.FillRegion;
import com.battleguess.battleguess.canvas.model.Line;
import com.battleguess.battleguess.enum_to_manage_string.CanvasToolType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import javafx.geometry.Point2D;

public class DrawingPane extends Canvas {
    private ArrayList<Line> lines;
    private ArrayList<FillRegion> fillRegions;
    private Color currentColor = Color.BLACK;
    private double currentStrokeSize = 1.0;
    private CanvasToolType currentTool = CanvasToolType.PENCIL;
    private double eraserSize = 5.0;
    private boolean isDrawingEnabled = true;

    public DrawingPane() {
        super(500, 400);
        getGraphicsContext2D().setFill(Color.WHITE);
        getGraphicsContext2D().fillRect(0, 0, getWidth(), getHeight());
        lines = new ArrayList<>();
        fillRegions = new ArrayList<>();
        setupMouseListeners();
    }

    private void setupMouseListeners() {
        setOnMousePressed(event -> {
            if (!isDrawingEnabled) return;

            if (currentTool.equals(CanvasToolType.PENCIL)) {
                Line newLine = new Line(currentColor, currentStrokeSize);
                newLine.addPoint(new Point2D(event.getX(), event.getY()));
                lines.add(newLine);
                redraw();
            } else if (currentTool.equals(CanvasToolType.ERASER)) {
                eraseAt(event.getX(), event.getY());
                redraw();
            } else if (currentTool.equals(CanvasToolType.FILL)) {
                FillRegion region = floodFill((int) event.getX(), (int) event.getY(), currentColor);
                if (region != null) {
                    fillRegions.add(region);
                }
                redraw();
            }
        });

        setOnMouseDragged(event -> {
            if (!isDrawingEnabled) return;

            if (currentTool.equals(CanvasToolType.PENCIL)) {
                lines.get(lines.size() - 1).addPoint(new Point2D(event.getX(), event.getY()));
                redraw();
            } else if (currentTool.equals(CanvasToolType.ERASER)) {
                eraseAt(event.getX(), event.getY());
                redraw();
            }
        });
    }

    private void eraseAt(double x, double y) {
        ArrayList<Line> newLines = new ArrayList<>();
        ArrayList<FillRegion> newFillRegions = new ArrayList<>();
        double eraserRadius = eraserSize / 2.0;

        for (Line line : lines) {
            ArrayList<Point2D> points = line.getPoints();
            if (points.size() < 2) {
                newLines.add(line);
                continue;
            }

            ArrayList<Point2D> currentSegment = new ArrayList<>();
            boolean inEraser = false;

            for (int i = 0; i < points.size(); i++) {
                Point2D point = points.get(i);
                boolean isInEraser = Math.abs(point.getX() - x) < eraserRadius && Math.abs(point.getY() - y) < eraserRadius;

                if (!isInEraser) {
                    if (inEraser && !currentSegment.isEmpty()) {
                        Line newLine = new Line(line.getColor(), line.getStrokeSize());
                        newLine.getPoints().addAll(currentSegment);
                        if (newLine.getPoints().size() >= 2) {
                            newLines.add(newLine);
                        }
                        currentSegment = new ArrayList<>();
                    }
                    currentSegment.add(point);
                    inEraser = false;
                } else {
                    if (!inEraser && !currentSegment.isEmpty()) {
                        Line newLine = new Line(line.getColor(), line.getStrokeSize());
                        newLine.getPoints().addAll(currentSegment);
                        if (newLine.getPoints().size() >= 2) {
                            newLines.add(newLine);
                        }
                        currentSegment = new ArrayList<>();
                    }
                    inEraser = true;
                }
            }

            if (!currentSegment.isEmpty() && currentSegment.size() >= 2) {
                Line newLine = new Line(line.getColor(), line.getStrokeSize());
                newLine.getPoints().addAll(currentSegment);
                newLines.add(newLine);
            }
        }

        for (FillRegion region : fillRegions) {
            ArrayList<Point2D> remainingPixels = new ArrayList<>();

            for (Point2D p : region.getFilledPixels()) {
                double dx = p.getX() - x;
                double dy = p.getY() - y;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > eraserRadius) {
                    remainingPixels.add(p);
                }
            }

            if (!remainingPixels.isEmpty()) {
                newFillRegions.add(new FillRegion(remainingPixels, region.getColor()));
            }
        }

        lines.clear();
        lines.addAll(newLines);
        fillRegions.clear();
        fillRegions.addAll(newFillRegions);
    }

    private FillRegion floodFill(int x, int y, Color fillColor) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) return null;

        WritableImage snapshot = new WritableImage((int) getWidth(), (int) getHeight());
        this.snapshot(null, snapshot);
        PixelReader reader = snapshot.getPixelReader();

        double threshold = 0.15;
        Color targetColor = reader.getColor(x, y);
        if (colorCloseEnough(targetColor, fillColor, threshold)) return null;

        int width = (int) getWidth();
        int height = (int) getHeight();
        boolean[][] visited = new boolean[width][height];
        Queue<Point2D> queue = new LinkedList<>();
        ArrayList<Point2D> filledPoints = new ArrayList<>();

        queue.add(new Point2D(x, y));

        while (!queue.isEmpty()) {
            Point2D p = queue.remove();
            int px = (int) p.getX();
            int py = (int) p.getY();

            if (px < 0 || py < 0 || px >= width || py >= height) continue;
            if (visited[px][py]) continue;

            Color current = reader.getColor(px, py);
            if (colorCloseEnough(current, targetColor, threshold)) {
                filledPoints.add(p);
                visited[px][py] = true;

                queue.add(new Point2D(px + 1, py));
                queue.add(new Point2D(px - 1, py));
                queue.add(new Point2D(px, py + 1));
                queue.add(new Point2D(px, py - 1));
            }
        }

        ArrayList<Point2D> expanded = new ArrayList<>();
        for (Point2D p : filledPoints) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    double nx = p.getX() + dx;
                    double ny = p.getY() + dy;
                    if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                        expanded.add(new Point2D(nx, ny));
                    }
                }
            }
        }
        filledPoints.addAll(expanded);

        return new FillRegion(filledPoints, fillColor);
    }

    private boolean colorCloseEnough(Color c1, Color c2, double threshold) {
        return Math.abs(c1.getRed() - c2.getRed()) < threshold &&
                Math.abs(c1.getGreen() - c2.getGreen()) < threshold &&
                Math.abs(c1.getBlue() - c2.getBlue()) < threshold;
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (FillRegion region : fillRegions) {
            gc.setFill(region.getColor());
            for (Point2D p : region.getFilledPixels()) {
                gc.fillRect(p.getX(), p.getY(), 1, 1);
            }
        }

        for (Line line : lines) {
            gc.setStroke(line.getColor());
            gc.setLineWidth(line.getStrokeSize());
            ArrayList<Point2D> points = line.getPoints();
            for (int i = 0; i < points.size() - 1; i++) {
                Point2D start = points.get(i);
                Point2D end = points.get(i + 1);
                gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
            }
        }
    }

    public void clearDrawing() {
        if (!isDrawingEnabled) return;

        forceClearCanvas();
    }

    public void forceClearCanvas() {
        lines.clear();
        fillRegions.clear();
        getGraphicsContext2D().setFill(Color.WHITE);
        getGraphicsContext2D().fillRect(0, 0, getWidth(), getHeight());
    }

    public CanvasToolType getCurrentTool() {
        return this.currentTool;
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public void setCurrentStrokeSize(double size) {
        this.currentStrokeSize = size;
    }

    public void setCurrentTool(CanvasToolType tool) {
        this.currentTool = tool;
    }

    public void setEraserSize(double size) {
        this.eraserSize = size;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.isDrawingEnabled = enabled;
    }

    public boolean isCanvasBlank() {
        return lines.isEmpty() && fillRegions.isEmpty();
    }

    public void loadPuzzleImage(byte[] imageData) {
        if (imageData == null) return;

        // Chuyển byte[] về Image
        javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageData));

        // Xóa canvas cũ
        lines.clear();
        fillRegions.clear();

        // Vẽ ảnh mới lên
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.drawImage(image, 0, 0);
    }
}