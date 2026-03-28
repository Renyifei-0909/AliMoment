package com.aiimoment.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML
    private HBox titleBar;
    @FXML
    private Button minimizeBtn;
    @FXML
    private Button maximizeBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private Button navMaterial;
    @FXML
    private Button navDraft;
    @FXML
    private Button navSearch;
    @FXML
    private Button navSmartEdit;
    @FXML
    private Button navSettings;
    @FXML
    private AnchorPane pageMaterial;
    @FXML
    private AnchorPane pageDraft;
    @FXML
    private AnchorPane pageSearch;
    @FXML
    private AnchorPane pageSmartEdit;
    @FXML
    private AnchorPane pageSettings;

    private Map<Button, AnchorPane> navMap = new HashMap<>();
    private Button activeNavButton;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        navMap.put(navMaterial, pageMaterial);
        navMap.put(navDraft, pageDraft);
        navMap.put(navSearch, pageSearch);
        navMap.put(navSmartEdit, pageSmartEdit);
        navMap.put(navSettings, pageSettings);

        initializePageContent();
        setupNavButtons();
        setupWindowControls();
        setupTitleBarDrag();

        setActiveNavButton(navMaterial);
    }

    private void initializePageContent() {
        loadPageFXML(pageMaterial, "/pages/MaterialPage.fxml");
        loadPageFXML(pageDraft, "/pages/DraftPage.fxml");
        loadPageFXML(pageSearch, "/pages/SearchPage.fxml");
        loadPageFXML(pageSmartEdit, "/pages/SmartEditPage.fxml");
        loadPageFXML(pageSettings, "/pages/SettingsPage.fxml");
    }

    private void loadPageFXML(AnchorPane container, String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Node page = loader.load();
            container.getChildren().add(page);
            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNavButtons() {
        navMap.keySet().forEach(button -> {
            button.setOnAction(event -> {
                setActiveNavButton(button);

                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), button);
                scaleTransition.setToX(0.95);
                scaleTransition.setToY(0.95);
                scaleTransition.setAutoReverse(true);
                scaleTransition.setCycleCount(2);
                scaleTransition.play();
            });

            button.setOnMouseEntered(event -> {
                if (button != activeNavButton) {
                    button.setStyle("-fx-background-color: rgba(102, 126, 234, 0.15); -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 500; -fx-padding: 12 24; -fx-background-radius: 8px;");
                }
            });

            button.setOnMouseExited(event -> {
                if (button != activeNavButton) {
                    button.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0a0; -fx-font-size: 16px; -fx-font-weight: 500; -fx-padding: 12 24; -fx-background-radius: 8px;");
                }
            });
        });
    }

    private void setActiveNavButton(Button button) {
        navMap.forEach((btn, page) -> {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0a0; -fx-font-size: 16px; -fx-font-weight: 500; -fx-padding: 12 24; -fx-background-radius: 8px;");
            page.setVisible(false);
        });

        activeNavButton = button;
        button.setStyle("-fx-background-color: rgba(102, 126, 234, 0.2); -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 600; -fx-padding: 12 24; -fx-background-radius: 8px;");
        navMap.get(button).setVisible(true);
    }

    private void setupWindowControls() {
        minimizeBtn.setOnAction(event -> {
            Stage stage = (Stage) minimizeBtn.getScene().getWindow();
            stage.setIconified(true);
        });

        maximizeBtn.setOnAction(event -> {
            Stage stage = (Stage) maximizeBtn.getScene().getWindow();
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });

        closeBtn.setOnAction(event -> {
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        });

        minimizeBtn.setOnMouseEntered(event -> {
            minimizeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });

        minimizeBtn.setOnMouseExited(event -> {
            minimizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0a0; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });

        maximizeBtn.setOnMouseEntered(event -> {
            maximizeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });

        maximizeBtn.setOnMouseExited(event -> {
            maximizeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0a0; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });

        closeBtn.setOnMouseEntered(event -> {
            closeBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });

        closeBtn.setOnMouseExited(event -> {
            closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0a0; -fx-font-size: 16px; -fx-pref-width: 32px; -fx-pref-height: 32px; -fx-background-radius: 6px;");
        });
    }

    private void setupTitleBarDrag() {
        titleBar.setOnMousePressed(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
    }
}