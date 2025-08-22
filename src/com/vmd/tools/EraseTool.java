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

    private final Canvas target;          
    private final Pane layers;           
    private final Runnable disablePan, enablePan;

    private final Pane glass = new Pane(); 
    private final HBox toolbar;
    private final Slider size = new Slider(5, 50, 20);
    private boolean visible = false;

    private final ImageCursor eraserCursor;

    public EraseTool(Canvas target, Pane layers, Runnable disablePan, Runnable enablePan, String iconPath) {
        this.target = target;
        this.layers = layers;
        this.disablePan = disablePan;
        this.enablePan = enablePan;

     
        this.eraserCursor = new ImageCursor(new Image(iconPath), 0, 0);

 
        Button close = new Button("✕");
        close.setOnAction(e -> hide());
        toolbar = new HBox(8, size, close);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-background-radius: 10;");
        toolbar.setLayoutX(20);
        toolbar.setLayoutY(100);

        size.setShowTickMarks(true);
        size.setShowTickLabels(true);

        glass.setPickOnBounds(true);
        glass.setMouseTransparent(true);
        glass.setStyle("-fx-background-color: transparent;");
        
        glass.prefWidthProperty().bind(layers.widthProperty());
        glass.prefHeightProperty().bind(layers.heightProperty());
    }

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

        Point2D p = target.sceneToLocal(glass.localToScene(e.getX(), e.getY()));

        gc.clearRect(p.getX() - s / 2, p.getY() - s / 2, s, s);

        e.consume();
    }
}
