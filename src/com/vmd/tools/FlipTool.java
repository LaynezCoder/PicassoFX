package com.vmd.tools;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;


public class FlipTool {

    private final ImageView baseView;
    private final Pane layers;
    private final HBox toolbar;

    private boolean visible = false;
    private boolean flippedH = false;
    private boolean flippedV = false;

    public FlipTool(ImageView baseView, Pane layers) {
        this.baseView = baseView;
        this.layers = layers;

        Button btnFlipH = new Button("Flip H");
        Button btnFlipV = new Button("Flip V");
        Button btnReset = new Button("Reset");
        Button btnClose = new Button("✕");

        btnFlipH.setOnAction(e -> flipH());
        btnFlipV.setOnAction(e -> flipV());
        btnReset.setOnAction(e -> reset());
        btnClose.setOnAction(e -> hide());

        toolbar = new HBox(8, btnFlipH, btnFlipV, btnReset, btnClose);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85);"
                + "-fx-background-radius: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);");

        String btnStyle = "-fx-text-fill: white; -fx-background-color: #3a3a3a; "
                + "-fx-background-radius: 8; -fx-font-weight: bold;";
        btnFlipH.setStyle(btnStyle);
        btnFlipV.setStyle(btnStyle);
        btnReset.setStyle(btnStyle);
        btnClose.setStyle("-fx-text-fill: white; -fx-background-color: #b02a37; -fx-background-radius: 8;");

        DoubleBinding centerX = layers.widthProperty().subtract(toolbar.widthProperty()).divide(2);
        toolbar.layoutXProperty().bind(centerX);
        toolbar.setLayoutY(56);
//        toolbar.setViewOrder(-100);
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
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    private void flipH() {
        flippedH = !flippedH;
        baseView.setScaleX(flippedH ? -1 : 1);
    }

    private void flipV() {
        flippedV = !flippedV;
        baseView.setScaleY(flippedV ? -1 : 1);
    }

    private void reset() {
        flippedH = false;
        flippedV = false;
        baseView.setScaleX(1);
        baseView.setScaleY(1);
    }
}
