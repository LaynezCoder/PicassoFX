package com.vmd;

import com.vmd.controllers.UploadViewController;
import com.vmd.tools.BrushTool;
import com.vmd.tools.ColorAdjustTool;
import com.vmd.tools.CropTool;
import com.vmd.tools.EraseTool;
import com.vmd.tools.FlipTool;
import com.vmd.tools.RotateTool;
import com.vmd.tools.StickerTool;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

public class FXMLDocumentController implements Initializable {

    @FXML
    private StackPane mainContainer;   // contenedor principal (ok)

    // Visor
    private ScrollPane viewer;
    private Group zoomGroup;           // nodo al que le aplicamos scale global
    private Pane layers;              // ⬅️ Pane (no StackPane) para evitar centrados
    private ImageView baseView;
    private Canvas paintLayer;

    // Transformaciones para pan & zoom consistentes
    private final Translate pan = new Translate(0, 0);
    private final Scale zoom = new Scale(1, 1, 0, 0); // pivot cambia en cada zoom

    // Overlay de subir
    private HBox uploadOverlay;

    // Estado drag
    private double dragStartX, dragStartY, startTX, startTY;

    private CropTool cropTool;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupViewerIfNeeded();
        loadUploadOverlay();

        cropTool = new com.vmd.tools.CropTool(layers, paintLayer, baseView, pan, zoom);
    }

    /* ===== llamado desde el hijo ===== */
    public void showImageInMainContainer(File file) {
        setupViewerIfNeeded();

        Image img = new Image(file.toURI().toString());
        baseView.setImage(img);

        // Capas del tamaño nativo
        paintLayer.setWidth(img.getWidth());
        paintLayer.setHeight(img.getHeight());
        GraphicsContext gc = paintLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, paintLayer.getWidth(), paintLayer.getHeight());

        // El Pane "layers" debe tener tamaño fijo a la imagen
        layers.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setPrefSize(img.getWidth(), img.getHeight());

        // Reset pan & zoom (origen arriba-izquierda, sin re-centrados)
        zoom.setX(1);
        zoom.setY(1);
        pan.setX(0);
        pan.setY(0);

        if (uploadOverlay != null) {
            uploadOverlay.setVisible(false);
        }
    }

    /* ===== visor + gestos ===== */
    private void setupViewerIfNeeded() {
        if (viewer != null) {
            return;
        }

        baseView = new ImageView();
        baseView.setPreserveRatio(true);
        baseView.setSmooth(true);

        paintLayer = new Canvas(1, 1);

        // Pane en (0,0); sin centrado automático
        layers = new Pane();
        layers.getChildren().addAll(baseView, paintLayer);
        layers.setPickOnBounds(true); // recibe drag aunque la imagen sea más pequeña

        // Grupo con transformaciones: primero pan, luego zoom
        zoomGroup = new Group(layers);
        zoomGroup.getTransforms().addAll(pan, zoom);

        viewer = new ScrollPane(zoomGroup);
        // ¡Muy importante! No ajustar al viewport:
        viewer.setFitToWidth(false);
        viewer.setFitToHeight(false);
        // Paneo lo haremos por drag, no por ScrollPane:
        viewer.setPannable(false);

        mainContainer.getChildren().setAll(viewer);

        // === Paneo por drag (libre, sin clamps) ===
        layers.setOnMousePressed(e -> {
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            startTX = pan.getX();
            startTY = pan.getY();
            layers.setCursor(Cursor.MOVE);
        });
        layers.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - dragStartX;
            double dy = e.getSceneY() - dragStartY;
            pan.setX(startTX + dx);
            pan.setY(startTY + dy);
        });
        layers.setOnMouseReleased(e -> layers.setCursor(Cursor.DEFAULT));

        // === Zoom suave al cursor ===
        zoomGroup.setOnScroll(e -> {
            double factor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
            double newScale = clamp(zoom.getX() * factor, 0.05, 20);

            // Punto bajo el cursor en coords de 'layers' (contenido)
            Point2D pivotInLayers = layers.sceneToLocal(e.getSceneX(), e.getSceneY());

            // Mantener el punto bajo el cursor "quieto" en pantalla:
            // 1) coordenadas actuales del pivot en el parent (después de pan+zoom actuales)
            Point2D before = layers.localToScene(pivotInLayers);

            // 2) aplicar nuevo zoom (pivot en (0,0); compensaremos con pan)
            zoom.setX(newScale);
            zoom.setY(newScale);

            // 3) coordenadas del mismo punto tras el zoom
            Point2D after = layers.localToScene(pivotInLayers);

            // 4) ajustar pan por la diferencia
            pan.setX(pan.getX() + (before.getX() - after.getX()));
            pan.setY(pan.getY() + (before.getY() - after.getY()));

            e.consume();
        });
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max ? max : v);
    }

    /* ===== overlay de upload ===== */
    private void loadUploadOverlay() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vmd/views/UploadView.fxml"));
            HBox upload = loader.load();
            UploadViewController child = loader.getController();
            child.setParentController(this);

            this.uploadOverlay = upload;

            upload.setMaxWidth(Region.USE_PREF_SIZE);
            upload.setMaxHeight(Region.USE_PREF_SIZE);
            upload.setPickOnBounds(false); // no bloquea drag fuera de sus hijos
            upload.setMouseTransparent(false);

            mainContainer.getChildren().add(upload);
            StackPane.setAlignment(upload, Pos.CENTER);

        } catch (IOException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void exportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Imagen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg)", "*.jpg")
        );

        File file = fileChooser.showSaveDialog(viewer.getScene().getWindow());
        if (file != null) {
            try {
                // 🔹 Tomar snapshot de "layers"
                WritableImage snapshot = layers.snapshot(new SnapshotParameters(), null);

                // Extensión según lo que elija el usuario
                String ext = getFileExtension(file.getName());
                if (ext == null || ext.isEmpty()) {
                    file = new File(file.getAbsolutePath() + ".png");
                    ext = "png";
                }

                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), ext, file);
                System.out.println("✅ Imagen exportada: " + file.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? "" : name.substring(dotIndex + 1);
    }

    public void openImageDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        // usa la ventana del root directamente (simple)
        File file = fc.showOpenDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            showImageInMainContainer(file);
        }
    }

    @FXML
    public void enterCropMode() {
        cropTool.enterCropMode();
    }

    private RotateTool rotateToolbar;

    @FXML
    private void onBtnRotarClick() {
        if (rotateToolbar == null) {
            rotateToolbar = new RotateTool(baseView, layers);
        }
        rotateToolbar.toggle(); // aparece/desaparece el toolbar
    }

    private FlipTool flipToolbar;

    @FXML
    private void onBtnFlipClick() {
        if (flipToolbar == null) {
            flipToolbar = new FlipTool(baseView, layers);
        }
        flipToolbar.toggle();
    }

    private ColorAdjustTool colorTool;

    @FXML
    private void onBtnFiltrosClick() {
        if (colorTool == null) {
            colorTool = new ColorAdjustTool(baseView, layers);
        }
        colorTool.toggle();
    }

    // Asumiendo que tu ScrollPane se llama 'viewer'
    private void disablePanZoom() {
        viewer.setPannable(false);
    }

    private void enablePanZoom() {
        viewer.setPannable(true);
    }

    private com.vmd.tools.BrushTool brushTool;
    private com.vmd.tools.EraseTool eraseTool;

    @FXML
    private void onBtnBrushClick() {
        if (eraseTool != null) {
            eraseTool.hide();
        }
        if (brushTool == null) {
            brushTool = new com.vmd.tools.BrushTool(paintLayer, layers, this::disablePanZoom, this::enablePanZoom);
        }
        brushTool.toggle();
    }

    @FXML
    private void onBtnEraseClick() {
        if (brushTool != null) {
            brushTool.hide();
        }
        if (eraseTool == null) {
            eraseTool = new com.vmd.tools.EraseTool(paintLayer, layers, this::disablePanZoom, this::enablePanZoom,
                    "/com/vmd/resources/eraser.png"); // ruta en resources
        }
        eraseTool.toggle();
    }

    private StickerTool stickerTool;

    @FXML
    private void onBtnStickersClick() {
        // apaga otras herramientas si están activas
        if (brushTool != null) {
            brushTool.hide();
        }
        if (eraseTool != null) {
            eraseTool.hide();
        }
        if (rotateToolbar != null) {
            rotateToolbar.hide();
        }
        if (flipToolbar != null) {
            flipToolbar.hide();
        }

        if (stickerTool == null) {
            stickerTool = new com.vmd.tools.StickerTool(layers, this::disablePanZoom, this::enablePanZoom);
        }
        stickerTool.toggle();
    }

}
