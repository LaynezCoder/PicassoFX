package com.vmd.tools;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * Filtros rápidos sobre un ImageView.
 * Uso:
 *   ColorAdjustTool fx = new ColorAdjustTool(baseView, layers);
 *   fx.toggle();  // mostrar/ocultar toolbar
 */
public class ColorAdjustTool {

    private final ImageView baseView;
    private final Pane layers;
    private final HBox toolbar;

    // Efectos
    private final ColorAdjust color = new ColorAdjust(); // brillo, contraste, saturación, hue
    private final SepiaTone sepia = new SepiaTone(0);    // 0 a 1
    private Effect chain; // color -> sepia

    private boolean visible = false;

    // Pasos simples
    private static final double STEP = 0.1;

    public ColorAdjustTool(ImageView baseView, Pane layers) {
        this.baseView = baseView;
        this.layers = layers;

        // Encadenar efectos: primero ColorAdjust, luego Sepia
        sepia.setInput(color);
        chain = sepia;
        baseView.setEffect(chain);

        // Botones
        Button bn       = btn("B/N", () -> setBlackWhite());
        Button sepiaBtn = btn("Sepia", () -> toggleSepia());
        Button bPlus    = btn("Br+ ", () -> incBrightness(+STEP));
        Button bMinus   = btn("Br- ", () -> incBrightness(-STEP));
        Button cPlus    = btn("Ct+ ", () -> incContrast(+STEP));
        Button cMinus   = btn("Ct- ", () -> incContrast(-STEP));
        Button sPlus    = btn("Sat+", () -> incSaturation(+STEP));
        Button sMinus   = btn("Sat-", () -> incSaturation(-STEP));
        Button reset    = btn("Reset", this::resetAll);
        Button close    = closeBtn();

        toolbar = new HBox(8, bn, sepiaBtn, bPlus, bMinus, cPlus, cMinus, sPlus, sMinus, reset, close);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85);"
                + "-fx-background-radius: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);");
//        toolbar.setViewOrder(-100);

        // Centrado arriba
        DoubleBinding centerX = layers.widthProperty().subtract(toolbar.widthProperty()).divide(2);
        toolbar.layoutXProperty().bind(centerX);
        toolbar.setLayoutY(100);
    }

    // === Mostrar / ocultar ===
    public void show() {
        if (!visible) {
            if (!layers.getChildren().contains(toolbar)) layers.getChildren().add(toolbar);
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

    public void toggle() { if (visible) hide(); else show(); }

    // === Acciones simples ===
    private void setBlackWhite() {
        sepia.setLevel(0);
        color.setSaturation(-1); // B/N directo
        color.setHue(0);
        color.setContrast(0);
        color.setBrightness(0);
    }

    private void toggleSepia() {
        // alterna entre 0 y 0.8
        sepia.setLevel(sepia.getLevel() > 0 ? 0 : 0.8);
    }

    private void incBrightness(double d) { color.setBrightness(clamp(color.getBrightness() + d)); }
    private void incContrast(double d)   { color.setContrast(clamp(color.getContrast() + d)); }
    private void incSaturation(double d) { color.setSaturation(clamp(color.getSaturation() + d)); }

    private void resetAll() {
        color.setBrightness(0);
        color.setContrast(0);
        color.setSaturation(0);
        color.setHue(0);
        sepia.setLevel(0);
    }

    // Utilidad
    private static double clamp(double v) { return Math.max(-1, Math.min(1, v)); }

    // Helpers UI
    private Button btn(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        b.setStyle("-fx-text-fill: white; -fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-font-weight: bold;");
        return b;
    }

    private Button closeBtn() {
        Button b = new Button("✕");
        b.setOnAction(e -> hide());
        b.setStyle("-fx-text-fill: white; -fx-background-color: #b02a37; -fx-background-radius: 8; -fx-font-weight: bold;");
        return b;
    }
}
