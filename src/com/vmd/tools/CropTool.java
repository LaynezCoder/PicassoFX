package com.vmd.tools;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class CropTool {

    private final Pane layers;     
    private final Canvas overlay;  
    private final ImageView baseView;
    private final Translate pan;   
    private final Scale zoom;     

    private boolean cropMode = false;
    private boolean hasSelection = false;
    private double selStartX, selStartY, selEndX, selEndY;

    private HBox toolbar;

    public CropTool(Pane layers, Canvas overlay, ImageView baseView, Translate pan, Scale zoom) {
        this.layers = layers;
        this.overlay = overlay;
        this.baseView = baseView;
        this.pan = pan;
        this.zoom = zoom;

        buildToolbar();
     
        this.overlay.setMouseTransparent(true);
        this.overlay.setPickOnBounds(true);
    }


    private void buildToolbar() {
        Button btnApply = new Button("Aplicar");
        Button btnCancel = new Button("Cancelar");

        btnApply.setOnAction(e -> applyCrop());
        btnCancel.setOnAction(e -> cancelCropMode());

        toolbar = new HBox(8, btnApply, btnCancel);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setStyle(
                "-fx-background-color: rgba(30,30,30,0.85);"
                + "-fx-background-radius: 8;"
                + "-fx-border-radius: 8;"
                + "-fx-border-color: rgba(255,255,255,0.20);"
                + "-fx-border-width: 1;"
        );
        toolbar.setVisible(false);

        layers.getChildren().add(toolbar);
        toolbar.setLayoutX(10);
        toolbar.setLayoutY(10);
    }

    public void enterCropMode() {
        if (baseView.getImage() == null || cropMode) {
            return;
        }

        reattachIfNeeded();

        cropMode = true;
        hasSelection = false;

        overlay.setWidth(baseView.getImage().getWidth());
        overlay.setHeight(baseView.getImage().getHeight());

        clearOverlay();

        overlay.toFront();
        toolbar.toFront();
        overlay.setMouseTransparent(false);
        overlay.setPickOnBounds(true);

        layers.setCursor(Cursor.CROSSHAIR);
        toolbar.setVisible(true);

        overlay.setOnMousePressed(e -> {
            if (!cropMode) {
                return;
            }
            Point2D p = layers.sceneToLocal(e.getSceneX(), e.getSceneY());
            selStartX = selEndX = p.getX();
            selStartY = selEndY = p.getY();
            hasSelection = false;
            drawOverlay();
            layers.setCursor(Cursor.CROSSHAIR);
            e.consume(); 
        });

        overlay.setOnMouseDragged(e -> {
            if (!cropMode) {
                return;
            }
            Point2D p = layers.sceneToLocal(e.getSceneX(), e.getSceneY());
            selEndX = p.getX();
            selEndY = p.getY();
            hasSelection = true;
            drawOverlay();
            e.consume();
        });

        overlay.setOnMouseReleased(e -> {
            if (!cropMode) {
                return;
            }
            layers.setCursor(Cursor.CROSSHAIR);
            e.consume();
        });
    }

   
    public void cancelCropMode() {
        exitCropModeClean();
    }

  
    public void applyCrop() {
        if (!cropMode || baseView.getImage() == null) {
            exitCropModeClean();
            return;
        }

        if (!hasSelection) {
            exitCropModeClean();
            return;
        }

        double x1 = Math.min(selStartX, selEndX);
        double y1 = Math.min(selStartY, selEndY);
        double x2 = Math.max(selStartX, selEndX);
        double y2 = Math.max(selStartY, selEndY);

        Image img = baseView.getImage();
        x1 = clamp(x1, 0, img.getWidth());
        y1 = clamp(y1, 0, img.getHeight());
        x2 = clamp(x2, 0, img.getWidth());
        y2 = clamp(y2, 0, img.getHeight());

        int w = (int) Math.round(x2 - x1);
        int h = (int) Math.round(y2 - y1);
        if (w <= 1 || h <= 1) {
            exitCropModeClean();
            return;
        }

        WritableImage cropped = new WritableImage(
                img.getPixelReader(), (int) Math.round(x1), (int) Math.round(y1), w, h);

        baseView.setImage(cropped);
        overlay.setWidth(cropped.getWidth());
        overlay.setHeight(cropped.getHeight());

        layers.setPrefSize(cropped.getWidth(), cropped.getHeight());
        layers.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        zoom.setX(1);
        zoom.setY(1);
        pan.setX(0);
        pan.setY(0);

        exitCropModeClean();
    }

    private void drawOverlay() {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

        double rx = Math.min(selStartX, selEndX);
        double ry = Math.min(selStartY, selEndY);
        double rw = Math.abs(selEndX - selStartX);
        double rh = Math.abs(selEndY - selStartY);

        gc.setFill(new Color(0, 0.5, 1, 0.20)); // azul translúcido
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);

        gc.fillRect(rx, ry, rw, rh);
        gc.strokeRect(rx + 0.5, ry + 0.5, Math.max(0, rw - 1), Math.max(0, rh - 1));
    }

    private void clearOverlay() {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
    }

    private void exitCropModeClean() {
        cropMode = false;
        hasSelection = false;
        clearOverlay();

        layers.setCursor(Cursor.DEFAULT);
        toolbar.setVisible(false);

        overlay.setMouseTransparent(true);
        overlay.setOnMousePressed(null);
        overlay.setOnMouseDragged(null);
        overlay.setOnMouseReleased(null);
    }

    private double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max ? max : v);
    }

    public void reattachIfNeeded() {
        if (toolbar.getParent() != layers) {
            layers.getChildren().add(toolbar);
        }
    }
}
