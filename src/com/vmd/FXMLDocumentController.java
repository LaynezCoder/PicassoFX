package com.vmd;

import com.vmd.controllers.UploadViewController;
import com.vmd.tools.BrushTool;
import com.vmd.tools.ColorAdjustTool;
import com.vmd.tools.CropTool;
import com.vmd.tools.EraseTool;
import com.vmd.tools.FlipTool;
import com.vmd.tools.RotateTool;
import com.vmd.tools.StickerTool;
import com.vmd.tools.TextTool;
import java.awt.Desktop;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

public class FXMLDocumentController implements Initializable {

    @FXML
    private StackPane mainContainer;   // contenedor principal

    // Visor
    private ScrollPane viewer;
    private Group zoomGroup;          
    private Pane layers;             
    private ImageView baseView;
    private Canvas paintLayer;

    // Transformaciones para pan & zoom consistentes
    private final Translate pan = new Translate(0, 0);
    private final Scale zoom = new Scale(1, 1, 0, 0); //

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

    public void showImageInMainContainer(File file) {
        setupViewerIfNeeded();

        Image img = new Image(file.toURI().toString());
        baseView.setImage(img);

        paintLayer.setWidth(img.getWidth());
        paintLayer.setHeight(img.getHeight());
        GraphicsContext gc = paintLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, paintLayer.getWidth(), paintLayer.getHeight());

        layers.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        layers.setPrefSize(img.getWidth(), img.getHeight());

        zoom.setX(1);
        zoom.setY(1);
        pan.setX(0);
        pan.setY(0);

        if (uploadOverlay != null) {
            uploadOverlay.setVisible(false);
        }
    }
    
    
    private void setupViewerIfNeeded() {
        if (viewer != null) {
            return;
        }

        baseView = new ImageView();
        baseView.setPreserveRatio(true);
        baseView.setSmooth(true);

        paintLayer = new Canvas(1, 1);

        layers = new Pane();
        layers.getChildren().addAll(baseView, paintLayer);
        layers.setPickOnBounds(true); 
  
        zoomGroup = new Group(layers);
        zoomGroup.getTransforms().addAll(pan, zoom);

        viewer = new ScrollPane(zoomGroup);
       
        viewer.setFitToWidth(false);
        viewer.setFitToHeight(false);
        
        viewer.setPannable(false);

        mainContainer.getChildren().setAll(viewer);

        
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

       
        zoomGroup.setOnScroll(e -> {
            double factor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
            double newScale = clamp(zoom.getX() * factor, 0.05, 20);

          
            Point2D pivotInLayers = layers.sceneToLocal(e.getSceneX(), e.getSceneY());

           
            Point2D before = layers.localToScene(pivotInLayers);

            
            zoom.setX(newScale);
            zoom.setY(newScale);

            
            Point2D after = layers.localToScene(pivotInLayers);

            pan.setX(pan.getX() + (before.getX() - after.getX()));
            pan.setY(pan.getY() + (before.getY() - after.getY()));

            e.consume();
        });
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max ? max : v);
    }

    private void loadUploadOverlay() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vmd/views/UploadView.fxml"));
            HBox upload = loader.load();
            UploadViewController child = loader.getController();
            child.setParentController(this);

            this.uploadOverlay = upload;

            upload.setMaxWidth(Region.USE_PREF_SIZE);
            upload.setMaxHeight(Region.USE_PREF_SIZE);
            upload.setPickOnBounds(false); 
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
    
                WritableImage snapshot = layers.snapshot(new SnapshotParameters(), null);

    
                String ext = getFileExtension(file.getName());
                if (ext == null || ext.isEmpty()) {
                    file = new File(file.getAbsolutePath() + ".png");
                    ext = "png";
                }

                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), ext, file);

                Alert ok = new Alert(AlertType.INFORMATION);
                ok.setTitle("Exportación completada");
                ok.setHeaderText(null);
                ok.setContentText("Imagen exportada:\n" + file.getAbsolutePath());
                ok.initOwner(viewer.getScene().getWindow());
                ok.showAndWait();

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }

            } catch (IOException e) {
                Alert err = new Alert(AlertType.ERROR);
                err.setTitle("Error al exportar");
                err.setHeaderText("No se pudo exportar la imagen");
                err.setContentText(e.getMessage());
                err.initOwner(viewer.getScene().getWindow());
                err.showAndWait();
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

        File file = fc.showOpenDialog(mainContainer.getScene().getWindow());
        if (file != null) {
            resetAll();
            showImageInMainContainer(file);
        }

        if (cropTool != null) {
            cropTool.reattachIfNeeded();
        }
    }

    private void resetAll() {
        pan.setX(0);
        pan.setY(0);
        zoom.setX(1);
        zoom.setY(1);

        GraphicsContext gc = paintLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, paintLayer.getWidth(), paintLayer.getHeight());

        layers.getChildren().clear();
        layers.getChildren().addAll(baseView, paintLayer);

        if (cropTool != null) {
            cropTool.reattachIfNeeded();
        }

        layers.setCursor(Cursor.DEFAULT);
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
        rotateToolbar.toggle();
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

    private TextTool textTool;

    @FXML
    private void onBtnTextClick() {
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
        if (stickerTool != null) {
            stickerTool.hide();
        }

        if (textTool == null) {
            textTool = new com.vmd.tools.TextTool(layers, baseView, this::disablePanZoom, this::enablePanZoom);
        }
        textTool.toggle();
    }

}
