/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vmd.controllers;

import com.vmd.FXMLDocumentController;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Laynecito
 */
public class UploadViewController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    private FXMLDocumentController parentController;

    public void setParentController(FXMLDocumentController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void openImageFile(MouseEvent e) {
        if (parentController != null) {
            parentController.openImageDialog();
        }
    }

}
