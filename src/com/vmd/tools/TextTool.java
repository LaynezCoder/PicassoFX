package com.vmd.tools;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class TextTool {

    private final Pane layers;
    private final ImageView baseView;
    private final Runnable disablePanZoom, enablePanZoom;

    private final ToolBar toolbar = new ToolBar();
    private final ColorPicker colorPicker = new ColorPicker(Color.WHITE);
    private final ComboBox<Integer> sizePicker = new ComboBox<>();
    private final Button btnAdd = new Button("Agregar");
    private final Button btnDelete = new Button("Eliminar");

    private Text selected = null;
    private boolean visible = false;

    public TextTool(Pane layers, ImageView baseView, Runnable disablePanZoom, Runnable enablePanZoom) {
        this.layers = layers;
        this.baseView = baseView;
        this.disablePanZoom = disablePanZoom;
        this.enablePanZoom = enablePanZoom;

        sizePicker.getItems().addAll(12, 16, 20, 24, 32, 40, 48, 64);
        sizePicker.getSelectionModel().select(Integer.valueOf(24));

        toolbar.getItems().addAll(new Label("Color:"), colorPicker,
                new Label("Tamaño:"), sizePicker, btnAdd, btnDelete);

        btnAdd.setOnAction(e -> enterAddMode());
        btnDelete.setOnAction(e -> {
            if (selected != null) {
                layers.getChildren().remove(selected);
                selected = null;
            }
        });
    }

    public void show() {
        if (visible) return;
        if (!layers.getChildren().contains(toolbar)) {
            layers.getChildren().add(toolbar);
            toolbar.setLayoutX(10);
            toolbar.setLayoutY(10);
        }
        disablePanZoom.run();
        visible = true;
        layers.requestFocus();
    }

    public void hide() {
        if (!visible) return;
        layers.getChildren().remove(toolbar);
        layers.setOnMouseClicked(null); // salir de modo agregar
        enablePanZoom.run();
        visible = false;
    }

    public void toggle() {
        if (visible) hide(); else show();
    }

    private void enterAddMode() {
        layers.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (!isInsideBase(e.getX(), e.getY())) return;

            Text t = new Text("Texto");
            t.setFont(Font.font(sizePicker.getValue()));
            t.setFill(colorPicker.getValue());
            t.setX(e.getX());
            t.setY(e.getY());

            // seleccionar + doble clic para editar
            t.setOnMouseClicked(ev -> {
                if (ev.getButton() == MouseButton.PRIMARY) {
                    selected = t;
                    if (ev.getClickCount() == 2) editText(t);
                    ev.consume();
                }
            });

            final double[] startText = new double[2];   
            final Point2D[] startMouse = new Point2D[1];

            t.setOnMousePressed(ev -> {
                if (!ev.isPrimaryButtonDown()) return;
    
                startText[0] = t.getX();
                startText[1] = t.getY();

                startMouse[0] = layers.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                disablePanZoom.run();  
                ev.consume();
            });

            t.setOnMouseDragged(ev -> {
                Point2D cur = layers.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                double dx = cur.getX() - startMouse[0].getX();
                double dy = cur.getY() - startMouse[0].getY();
                t.setX(startText[0] + dx);
                t.setY(startText[1] + dy);
                clampInside(t);
                ev.consume();
            });

            t.setOnMouseReleased(ev -> {
                enablePanZoom.run();
                ev.consume();
            });

            layers.getChildren().add(t);
            selected = t;


            layers.setOnMouseClicked(null);
        });
    }

    private void editText(Text t) {
        TextInputDialog d = new TextInputDialog(t.getText());
        d.setTitle("Editar texto");
        d.setHeaderText(null);
        d.setContentText("Nuevo texto:");
        d.showAndWait().ifPresent(newTxt -> {
            t.setText(newTxt.isEmpty() ? "Texto" : newTxt);
            t.setFont(Font.font(sizePicker.getValue()));
            t.setFill(colorPicker.getValue());
            clampInside(t);
        });
    }

    private boolean isInsideBase(double x, double y) {
        Bounds b = baseView.getBoundsInParent();
        return x >= b.getMinX() && x <= b.getMaxX() && y >= b.getMinY() && y <= b.getMaxY();
    }

    private void clampInside(Text t) {
        Bounds b = baseView.getBoundsInParent();
        double w = t.getLayoutBounds().getWidth();
        double h = t.getLayoutBounds().getHeight();

        double minX = b.getMinX();
        double minY = b.getMinY() + h; 
        double maxX = b.getMaxX() - w;
        double maxY = b.getMaxY();

        if (t.getX() < minX) t.setX(minX);
        if (t.getX() > maxX) t.setX(maxX);
        if (t.getY() < minY) t.setY(minY);
        if (t.getY() > maxY) t.setY(maxY);
    }
}
