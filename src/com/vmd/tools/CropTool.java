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

    // Referencias del editor
    private final Pane layers;     // Pane que contiene baseView + overlay
    private final Canvas overlay;  // Canvas usado como overlay (paintLayer)
    private final ImageView baseView;
    private final Translate pan;   // pan global del editor
    private final Scale zoom;      // zoom global del editor

    // Estado de recorte
    private boolean cropMode = false;
    private boolean hasSelection = false;
    private double selStartX, selStartY, selEndX, selEndY;

    // Mini toolbar (Aplicar/Cancelar) dentro de 'layers'
    private HBox toolbar;

    public CropTool(Pane layers, Canvas overlay, ImageView baseView, Translate pan, Scale zoom) {
        this.layers   = layers;
        this.overlay  = overlay;
        this.baseView = baseView;
        this.pan      = pan;
        this.zoom     = zoom;

        buildToolbar();
        // Por defecto el overlay NO intercepta eventos
        this.overlay.setMouseTransparent(true);
        this.overlay.setPickOnBounds(true);
    }

    /** Construye y agrega la mini‑toolbar dentro del área de la imagen */
    private void buildToolbar() {
        Button btnApply  = new Button("Aplicar");
        Button btnCancel = new Button("Cancelar");

        btnApply.setOnAction(e -> applyCrop());
        btnCancel.setOnAction(e -> cancelCropMode());

        toolbar = new HBox(8, btnApply, btnCancel);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setStyle(
            "-fx-background-color: rgba(30,30,30,0.85);" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-width: 1;"
        );
        toolbar.setVisible(false);

        // Agregar dentro de 'layers' (se ubica respecto a la imagen)
        layers.getChildren().add(toolbar);
        toolbar.setLayoutX(10);
        toolbar.setLayoutY(10);
    }

    /** Activar modo recorte: muestra toolbar, trae overlay al frente y captura eventos */
    public void enterCropMode() {
        if (baseView.getImage() == null || cropMode) return;

        cropMode = true;
        hasSelection = false;

        // Asegurar que el overlay cubra la imagen actual
        overlay.setWidth(baseView.getImage().getWidth());
        overlay.setHeight(baseView.getImage().getHeight());

        clearOverlay();

        // Traer al frente y capturar eventos
        overlay.toFront();
        toolbar.toFront();
        overlay.setMouseTransparent(false);
        overlay.setPickOnBounds(true);

        layers.setCursor(Cursor.CROSSHAIR);
        toolbar.setVisible(true);

        // Handlers SOLO en el overlay (no tocamos los de 'layers' → pan intacto)
        overlay.setOnMousePressed(e -> {
            if (!cropMode) return;
            Point2D p = layers.sceneToLocal(e.getSceneX(), e.getSceneY());
            selStartX = selEndX = p.getX();
            selStartY = selEndY = p.getY();
            hasSelection = false;
            drawOverlay();
            layers.setCursor(Cursor.CROSSHAIR);
            e.consume(); // ← evita que el pan reciba el evento
        });

        overlay.setOnMouseDragged(e -> {
            if (!cropMode) return;
            Point2D p = layers.sceneToLocal(e.getSceneX(), e.getSceneY());
            selEndX = p.getX();
            selEndY = p.getY();
            hasSelection = true;
            drawOverlay();
            e.consume(); // ← evita pan
        });

        overlay.setOnMouseReleased(e -> {
            if (!cropMode) return;
            layers.setCursor(Cursor.CROSSHAIR);
            e.consume(); // ← evita pan
        });
    }

    /** Cancelar modo recorte (siempre restaura control de pan/click) */
    public void cancelCropMode() {
        exitCropModeClean();
    }

    /** Aplicar recorte si hay selección válida, si no, salir limpio sin cambios */
    public void applyCrop() {
        if (!cropMode || baseView.getImage() == null) {
            exitCropModeClean();
            return;
        }

        // Si no hubo arrastre/selección, salir sin alterar la imagen
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

        int w = (int)Math.round(x2 - x1);
        int h = (int)Math.round(y2 - y1);
        if (w <= 1 || h <= 1) {
            exitCropModeClean();
            return;
        }

        WritableImage cropped = new WritableImage(
                img.getPixelReader(), (int)Math.round(x1), (int)Math.round(y1), w, h);

        // Reemplazar imagen y ajustar tamaños de capas
        baseView.setImage(cropped);
        overlay.setWidth(cropped.getWidth());
        overlay.setHeight(cropped.getHeight());

        layers.setPrefSize(cropped.getWidth(), cropped.getHeight());
        layers.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Reset de pan/zoom (opcional pero recomendado tras recorte)
        zoom.setX(1); zoom.setY(1);
        pan.setX(0);  pan.setY(0);

        exitCropModeClean();
    }

    // ================== Helpers internos ==================

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

    /** Salida limpia centralizada: restaura cursor, toolbar, overlay y eventos */
    private void exitCropModeClean() {
        cropMode = false;
        hasSelection = false;
        clearOverlay();

        layers.setCursor(Cursor.DEFAULT);
        toolbar.setVisible(false);

        // Liberar overlay para que no bloquee clics del pan
        overlay.setMouseTransparent(true);
        overlay.setOnMousePressed(null);
        overlay.setOnMouseDragged(null);
        overlay.setOnMouseReleased(null);
    }

    private double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max ? max : v);
    }
}
