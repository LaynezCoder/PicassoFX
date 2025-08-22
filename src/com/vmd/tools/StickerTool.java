package com.vmd.tools;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class StickerTool {

    private final Pane layers;                   // tu Pane principal "layers"
    private final Pane stickerLayer = new Pane();// capa de stickers (ImageViews)
    private final Runnable disablePan, enablePan;

    // Toolbar
    private final HBox toolbar;
    private final ComboBox<String> presets = new ComboBox<>();
    private final Button btnAddPreset = new Button("Agregar");
    private final Button btnAddFile   = new Button("Archivo...");
    private final Button btnDelete    = new Button("Borrar");
    private final Button btnClose     = new Button("✕");

    private boolean visible = false;
    private ImageView selected = null;

    // Nombre -> ruta CLASSPATH (empieza con /)
    private final Map<String, String> presetPaths = new HashMap<>();

    public StickerTool(Pane layers, Runnable disablePan, Runnable enablePan) {
        this.layers = layers;
        this.disablePan = disablePan;
        this.enablePan  = enablePan;

        // ==== PRESETS (ajusta a tus rutas reales en resources) ====
        presetPaths.put("Estrella", "/com/vmd/resources/stickers/star.png");
        presetPaths.put("Corazón",  "/com/vmd/resources/stickers/heart.png");
        presetPaths.put("Flecha",   "/com/vmd/resources/stickers/arrow.png");

        presets.getItems().addAll(presetPaths.keySet());
        presets.setPromptText("Stickers");
        presets.setTooltip(new Tooltip("Selecciona un sticker"));

        btnAddPreset.setOnAction(e -> addPreset());
        btnAddFile.setOnAction(e -> addFromFile());
        btnDelete.setOnAction(e -> deleteSelected());
        btnClose.setOnAction(e -> hide());

        toolbar = new HBox(8, presets, btnAddPreset, btnAddFile, btnDelete, btnClose);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-background-radius: 10;");
        toolbar.setLayoutX(20);
        toolbar.setLayoutY(140);
//        toolbar.setViewOrder(-100); // SIEMPRE encima

        // Capa de stickers
        stickerLayer.setPickOnBounds(false);
        stickerLayer.setMouseTransparent(true); // se activa en show()
        stickerLayer.prefWidthProperty().bind(layers.widthProperty());
        stickerLayer.prefHeightProperty().bind(layers.heightProperty());
//        stickerLayer.setViewOrder(-30); // por delante del paintLayer/baseView

        if (!layers.getChildren().contains(stickerLayer)) {
            layers.getChildren().add(stickerLayer);
        }
    }

    // ===== Ciclo de vida =====
    public void show() {
        if (visible) return;
        if (!layers.getChildren().contains(toolbar)) layers.getChildren().add(toolbar);
        toolbar.setVisible(true);

        // traer al frente por si otras capas usan viewOrder similar
        stickerLayer.toFront();
        toolbar.toFront();

        stickerLayer.setMouseTransparent(false);
        disablePan.run(); // desactiva pan mientras editas
        visible = true;
    }

    public void hide() {
        if (!visible) return;
        toolbar.setVisible(false);
        stickerLayer.setMouseTransparent(true);
        clearSelection();
        enablePan.run();
        visible = false;
    }

    public void toggle() { if (visible) hide(); else show(); }

    // ===== Acciones =====
    private void addPreset() {
        String name = presets.getSelectionModel().getSelectedItem();
        if (name == null) return;
        Image img = loadResource(presetPaths.get(name));   // **classpath**
        if (img != null) addSticker(img);
    }

    private void addFromFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imágenes", "*.png","*.jpg","*.jpeg","*.gif")
        );
        File f = fc.showOpenDialog(layers.getScene().getWindow());
        if (f != null) {
            // **SIN background** para tener width/height ya mismo
            Image img = new Image(f.toURI().toString(), false);
            addSticker(img);
        }
    }

    // Carga de CLASSPATH: "/com/vmd/resources/stickers/star.png"
    private Image loadResource(String resourcePath) {
        URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("No se encontró recurso: " + resourcePath);
            return null;
        }
        return new Image(url.toExternalForm(), false); // sin background
    }

    private void addSticker(Image img) {
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);

        // 1) Tamaño inicial (25% del ancho visible, con límites, y sin depender del background loading)
        double initW = Math.min(layers.getWidth() * 0.25, 240);
        if (initW <= 0) initW = 160; // fallback si aún no hay layout
        iv.setFitWidth(Math.min(initW, img.getWidth() > 0 ? img.getWidth() : initW));

        // 2) Centrar y asegurar dentro
        iv.setTranslateX((layers.getWidth() - widthOf(iv)) / 2);
        iv.setTranslateY((layers.getHeight() - heightOf(iv)) / 2);
        clampInside(iv);

        iv.setCursor(Cursor.OPEN_HAND);

        // Selección y arrastre
        iv.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                select(iv);
                iv.setCursor(Cursor.CLOSED_HAND);
                iv.setUserData(new double[]{e.getX(), e.getY()}); // offset interno
                e.consume();
            }
        });

        iv.setOnMouseReleased(e -> { iv.setCursor(Cursor.OPEN_HAND); e.consume(); });

        iv.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double[] off = (double[]) iv.getUserData();
                iv.setTranslateX(iv.getTranslateX() + (e.getX() - off[0]));
                iv.setTranslateY(iv.getTranslateY() + (e.getY() - off[1]));
                clampInside(iv);
                e.consume();
            }
        });

        // 3) Scroll = escalar con límites y mantener dentro
        iv.addEventFilter(ScrollEvent.SCROLL, e -> {
            double factor = (e.getDeltaY() > 0) ? 1.10 : 0.90;
            double minW = 48;                                    // mínimo visible
            double maxW = Math.max(96, layers.getWidth() * 0.60); // máximo relativo
            double newW = clamp(iv.getFitWidth() * factor, minW, maxW);
            iv.setFitWidth(newW);
            clampInside(iv);
            e.consume();
        });

        // Doble clic = al frente
        iv.setOnMouseClicked(e -> { if (e.getClickCount() == 2) iv.toFront(); });

        stickerLayer.getChildren().add(iv);
        select(iv);
    }

    // ===== Util =====
    private void select(ImageView iv) {
        if (selected == iv) return;
        clearSelection();
        selected = iv;
        selected.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 2);");
    }

    private void clearSelection() {
        if (selected != null) {
            selected.setStyle(null);
            selected = null;
        }
    }

    private void deleteSelected() {
        if (selected != null) {
            stickerLayer.getChildren().remove(selected);
            selected = null;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double widthOf(ImageView iv) {
        return iv.getFitWidth(); // preserveRatio=true → ancho visible = fitWidth
    }

    private static double heightOf(ImageView iv) {
        Image img = iv.getImage();
        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return iv.getFitWidth(); // fallback
        return iv.getFitWidth() * (ih / iw);
    }

    /** Mantiene el sticker dentro del área visible de 'layers'. */
    private void clampInside(ImageView iv) {
        double w = widthOf(iv);
        double h = heightOf(iv);

        double minX = 0;
        double minY = 0;
        double maxX = Math.max(0, layers.getWidth()  - w);
        double maxY = Math.max(0, layers.getHeight() - h);

        double x = iv.getTranslateX();
        double y = iv.getTranslateY();

        if (x < minX) x = minX;
        if (y < minY) y = minY;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;

        iv.setTranslateX(x);
        iv.setTranslateY(y);
    }

    public Pane getStickerLayer() { return stickerLayer; }
}
