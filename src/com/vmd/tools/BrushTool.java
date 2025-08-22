package com.vmd.tools;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class BrushTool {

    private final Canvas canvas;
    private final Pane layers;
    private final Runnable disablePan, enablePan;
    private final HBox toolbar;
    private final GraphicsContext gc;

    private boolean visible = false;
    private final ColorPicker color = new ColorPicker(Color.RED);
    private final Slider size = new Slider(1, 30, 6);

    public BrushTool(Canvas canvas, Pane layers, Runnable disablePan, Runnable enablePan) {
        this.canvas = canvas;
        this.layers = layers;
        this.disablePan = disablePan;
        this.enablePan = enablePan;

        gc = canvas.getGraphicsContext2D();
        // Config permanente del pincel (cap/join redondos)
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        Button clear = new Button("Borrar Todo");
        Button close = new Button("✕");
        clear.setOnAction(e -> gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));
        close.setOnAction(e -> hide());

        toolbar = new HBox(8, color, size, clear, close);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-background-radius: 10;");
        toolbar.setLayoutX(20);
        toolbar.setLayoutY(20);
//        toolbar.setViewOrder(-100);

        canvas.setPickOnBounds(true);
//        canvas.setViewOrder(-10);
        canvas.setMouseTransparent(true);
    }

    public void show() {
        if (visible) return;
        if (!layers.getChildren().contains(toolbar)) layers.getChildren().add(toolbar);
        toolbar.setVisible(true);
        canvas.setMouseTransparent(false);
        enableHandlers(true);
        disablePan.run();
        visible = true;
    }

    public void hide() {
        if (!visible) return;
        toolbar.setVisible(false);
        enableHandlers(false);
        canvas.setMouseTransparent(true);
        enablePan.run();
        visible = false;
    }

    public void toggle() { if (visible) hide(); else show(); }

    private void enableHandlers(boolean on) {
        if (on) {
            canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::start);
            canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::drag);
            canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> e.consume());
        } else {
            canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::start);
            canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, this::drag);
        }
    }

    private void start(MouseEvent e) {
        // Forzar estado sano tras EraseTool
        gc.setGlobalAlpha(1.0);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setEffect(null);                 // por si algún efecto quedó activo
        gc.setStroke(color.getValue());
        gc.setLineWidth(size.getValue());

        // Trazado CONTINUO (sin miles de strokeLine)
        gc.beginPath();
        gc.moveTo(e.getX(), e.getY());
        gc.stroke();

        e.consume();
    }

    private void drag(MouseEvent e) {
        gc.setGlobalAlpha(1.0);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setStroke(color.getValue());
        gc.setLineWidth(size.getValue());

        gc.lineTo(e.getX(), e.getY());
        gc.stroke();   // pinta el tramo acumulado suavemente

        e.consume();
    }
}
