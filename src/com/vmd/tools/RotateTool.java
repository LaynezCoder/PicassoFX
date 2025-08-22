package com.vmd.tools;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class RotateTool {

    private final ImageView baseView;
    private final Pane layers;
    private final HBox toolbar;

    private int angle = 0;
    private boolean visible = false;

    public RotateTool(ImageView baseView, Pane layers) {
        this.baseView = baseView;
        this.layers = layers;

        // Botones
        Button btnLeft  = new Button("⟲ 90°");
        Button btnRight = new Button("⟳ 90°");
        Button btnReset = new Button("Reset");
        Button btnClose = new Button("✕");

        // Acciones
        btnLeft.setOnAction(e -> rotateLeft());
        btnRight.setOnAction(e -> rotateRight());
        btnReset.setOnAction(e -> resetRotation());
        btnClose.setOnAction(e -> hide());

        toolbar = new HBox(8, btnLeft, btnRight, btnReset, btnClose);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85);"
                       + "-fx-background-radius: 10;"
                       + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);");
        toolbar.setPickOnBounds(true);

        String btnStyle = "-fx-text-fill: white; -fx-background-color: #3a3a3a; "
                        + "-fx-background-radius: 8; -fx-font-weight: bold;";
        btnLeft.setStyle(btnStyle);
        btnRight.setStyle(btnStyle);
        btnReset.setStyle(btnStyle);
        btnClose.setStyle("-fx-text-fill: white; -fx-background-color: #b02a37; -fx-background-radius: 8;");

        DoubleBinding centerX = layers.widthProperty().subtract(toolbar.widthProperty()).divide(2);
        toolbar.layoutXProperty().bind(centerX);
        toolbar.setLayoutY(12);

    }

    public void show() {
        if (!visible) {
            if (!layers.getChildren().contains(toolbar)) {
                layers.getChildren().add(toolbar);
            }
            toolbar.setVisible(true);
            visible = true;
        }
    }

    public void hide() {
        if (visible) {
            toolbar.setVisible(false);
            visible = false;
        }
    }

    public void toggle() {
        if (visible) hide(); else show();
    }

    private void rotateRight() {
        angle = (angle + 90) % 360;
        baseView.setRotate(angle);
    }

    private void rotateLeft() {
        angle = (angle - 90 + 360) % 360;
        baseView.setRotate(angle);
    }

    private void resetRotation() {
        angle = 0;
        baseView.setRotate(angle);
    }
}
