package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.canvas.DrawingPane;
import com.battleguess.battleguess.enum_to_manage_string.CanvasToolType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class CanvasController {
    @FXML public DrawingPane drawingPane;
    @FXML private Slider sizeSlider;
    @FXML private ColorPicker colorPicker;
    @FXML private Button pencilTool;
    @FXML private Button eraserTool;
    @FXML private Button fillTool;
    @FXML private VBox toolsVBox;

    @FXML
    private void initialize() {
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSize(newVal.doubleValue());
        });

        colorPicker.setValue(Color.BLACK);
        colorPicker.setOnAction(event -> {
            drawingPane.setCurrentColor(colorPicker.getValue());
        });

        colorPicker.setValue(Color.BLACK);
        colorPicker.setOnAction(event -> {
            drawingPane.setCurrentColor(colorPicker.getValue());
        });

        selectPencilTool();
    }

    @FXML
    private void selectPencilTool() {
        drawingPane.setCurrentTool(CanvasToolType.PENCIL);
        drawingPane.setCurrentStrokeSize(sizeSlider.getValue());
        setActiveButton(pencilTool);
    }

    @FXML
    private void selectEraserTool() {
        drawingPane.setCurrentTool(CanvasToolType.ERASER);
        drawingPane.setEraserSize(sizeSlider.getValue());
        setActiveButton(eraserTool);
    }

    @FXML
    private void selectFillTool() {
        drawingPane.setCurrentTool(CanvasToolType.FILL);
        drawingPane.setCurrentColor(colorPicker.getValue());
        setActiveButton(fillTool);
    }

    @FXML
    private void selectColor() {
        drawingPane.setCurrentColor(colorPicker.getValue());
    }

    @FXML
    public void clearDrawing() {
        drawingPane.clearDrawing();
    }

    public void forceClearDrawing() {
        drawingPane.forceClearCanvas();
    }

    public void setDrawingEnabled(boolean enabled) {
        drawingPane.setDrawingEnabled(enabled);
    }

    private void updateSize(double newSize) {
        drawingPane.setCurrentStrokeSize(newSize);
        drawingPane.setEraserSize(newSize);
    }

    public boolean isCanvasBlank() {
        return drawingPane.isCanvasBlank();
    }

    public void loadPuzzleImage(byte[] imageData) {
        drawingPane.loadPuzzleImage(imageData);
    }

    private void setActiveButton(Button activeButton) {
        pencilTool.getStyleClass().remove("active-tool");
        eraserTool.getStyleClass().remove("active-tool");
        fillTool.getStyleClass().remove("active-tool");

        if (!activeButton.getStyleClass().contains("active-tool")) {
            activeButton.getStyleClass().add("active-tool");
        }
    }

    public WritableImage getSnapshot() {
        WritableImage img = new WritableImage((int)drawingPane.getWidth(), (int)drawingPane.getHeight());
        drawingPane.snapshot(null, img);
        return img;
    }

    public void showTools(boolean show) {
        if (toolsVBox != null) {
            toolsVBox.setVisible(show);
            toolsVBox.setManaged(show);
        }
    }
}