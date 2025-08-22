package com.vmd.tools;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class EraseTool {

    private final Canvas target;          // tu paintLayer
    private final Pane layers;            // contenedor donde están baseView + paintLayer
    private final Runnable disablePan, enablePan;

    private final Pane glass = new Pane(); // overlay que captura el mouse
    private final HBox toolbar;
    private final Slider size = new Slider(5, 50, 20);
    private boolean visible = false;

    private final ImageCursor eraserCursor;

    public EraseTool(Canvas target, Pane layers, Runnable disablePan, Runnable enablePan, String iconPath) {
        this.target = target;
        this.layers = layers;
        this.disablePan = disablePan;
        this.enablePan = enablePan;

        // Cursor personalizado (pon el recurso en /icons/eraser.png, p. ej.)
        this.eraserCursor = new ImageCursor(new Image(iconPath), 0, 0);

        // Toolbar mínima
        Button close = new Button("✕");
        close.setOnAction(e -> hide());
        toolbar = new HBox(8, size, close);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-background-radius: 10;");
        toolbar.setLayoutX(20);
        toolbar.setLayoutY(100);
//        toolbar.setViewOrder(-100);

        size.setShowTickMarks(true);
        size.setShowTickLabels(true);

        // Overlay (capa de vidrio): cubre el área y captura eventos
        glass.setPickOnBounds(true);
        glass.setMouseTransparent(true); // inactivo hasta show()
        glass.setStyle("-fx-background-color: transparent;");
//        glass.setViewOrder(-15);         // por encima del canvas, debajo de toolbar
        // que siempre cubra el área visible
        glass.prefWidthProperty().bind(layers.widthProperty());
        glass.prefHeightProperty().bind(layers.heightProperty());
    }

    // ========= ciclo de vida =========
    public void show() {
        if (visible) {
            return;
        }
        if (!layers.getChildren().contains(glass)) {
            layers.getChildren().add(glass);
        }
        if (!layers.getChildren().contains(toolbar)) {
            layers.getChildren().add(toolbar);
        }
        toolbar.setVisible(true);

        glass.setMouseTransparent(false);
        glass.setCursor(eraserCursor);
        enableHandlers(true);
        disablePan.run();
        visible = true;
    }

    public void hide() {
        if (!visible) {
            return;
        }
        toolbar.setVisible(false);
        glass.setMouseTransparent(true);
        glass.setCursor(Cursor.DEFAULT);
        enableHandlers(false);
        enablePan.run();
        visible = false;
    }

    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    // ========= eventos en el OVERLAY, no en el canvas =========
    private void enableHandlers(boolean on) {
        if (on) {
            glass.addEventHandler(MouseEvent.MOUSE_PRESSED, this::erase);
            glass.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::erase);
            glass.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> e.consume());
        } else {
            glass.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::erase);
            glass.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::erase);
        }
    }

    private void erase(MouseEvent e) {
        GraphicsContext gc = target.getGraphicsContext2D();
        double s = size.getValue();

        // Convertir coords del overlay -> al canvas (robusto con pan/zoom)
        Point2D p = target.sceneToLocal(glass.localToScene(e.getX(), e.getY()));

        // Borrador redondo simple (sin stroke, sin blend)
        gc.clearRect(p.getX() - s / 2, p.getY() - s / 2, s, s);

        e.consume(); // nadie más procesa este evento (el brush no se dispara)
    }
}
