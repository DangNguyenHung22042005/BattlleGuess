package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.canvas.DrawingPane;
import com.battleguess.battleguess.enum_to_manage_string.CanvasToolType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;

public class CanvasController {
    @FXML
    public DrawingPane drawingPane;
    @FXML
    private ChoiceBox<Double> pencilSize;
    @FXML
    private ChoiceBox<Double> eraserSize;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Button pencilTool;
    @FXML
    private Button eraserTool;
    @FXML
    private Button fillTool;
    @FXML
    private Button toggleDrawing;
    private boolean drawingEnabled = true;

    @FXML
    private void initialize() {
        pencilSize.setItems(FXCollections.observableArrayList(1.0, 2.0, 3.0, 4.0, 5.0));
        pencilSize.setValue(1.0);
        pencilSize.setOnAction(event -> {
            drawingPane.setCurrentStrokeSize(pencilSize.getValue());
            selectPencilTool();
        });

        eraserSize.setItems(FXCollections.observableArrayList(5.0, 10.0, 15.0, 20.0, 25.0));
        eraserSize.setValue(5.0);
        eraserSize.setOnAction(event -> {
            drawingPane.setEraserSize(eraserSize.getValue());
            selectEraserTool();
        });

        colorPicker.setValue(Color.BLACK);
        colorPicker.setOnAction(event -> {
            drawingPane.setCurrentColor(colorPicker.getValue());
        });

        updateToggleButtonStyle();

        selectPencilTool();
    }

    @FXML
    private void selectPencilTool() {
        drawingPane.setCurrentTool(CanvasToolType.PENCIL);
        drawingPane.setCurrentStrokeSize(pencilSize.getValue());
        setActiveButton(pencilTool);
    }

    @FXML
    private void selectEraserTool() {
        drawingPane.setCurrentTool(CanvasToolType.ERASER);
        drawingPane.setEraserSize(eraserSize.getValue());
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
    private void clearDrawing() {
        drawingPane.clearDrawing();
    }

    @FXML
    private void toggleDrawing() {
        drawingEnabled = !drawingEnabled;
        drawingPane.setDrawingEnabled(drawingEnabled);
        toggleDrawing.setText(drawingEnabled ? "Disable Drawing" : "Enable Drawing");
        updateToggleButtonStyle();
    }

    private void updateToggleButtonStyle() {
        toggleDrawing.getStyleClass().remove("active-tool");
        if (drawingEnabled) {
            toggleDrawing.getStyleClass().add("active-tool");
        }
    }

    private void setActiveButton(Button activeButton) {
        pencilTool.getStyleClass().remove("active-tool");
        eraserTool.getStyleClass().remove("active-tool");
        fillTool.getStyleClass().remove("active-tool");

        if (!activeButton.getStyleClass().contains("active-tool")) {
            activeButton.getStyleClass().add("active-tool");
        }
    }
}