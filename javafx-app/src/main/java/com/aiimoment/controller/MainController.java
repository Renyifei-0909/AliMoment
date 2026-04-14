package com.aiimoment.controller;

import javafx.animation.ScaleTransition;
import com.aiimoment.ui.AliBrandLogo;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    private static final String NAV_STYLE = "nav-tab-btn";

    @FXML
    private BorderPane topChrome;
    @FXML
    private StackPane brandLogoContainer;
    @FXML
    private Button navMenuBtn;
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
        setupTopChromeDrag();

        if (navMenuBtn != null) {
            navMenuBtn.setOnAction(e -> { /* 菜单占位 */ });
        }

        loadBrandLogo();
        setActiveNavButton(navSearch);
    }

    private void loadBrandLogo() {
        if (brandLogoContainer == null) {
            return;
        }
        var url = getClass().getResource("/images/ali_logo.png");
        if (url == null) {
            url = getClass().getResource("/images/ali-logo.png");
        }
        if (url != null) {
            ImageView iv = new ImageView(new Image(url.toExternalForm(), 32, 32, true, true, true));
            brandLogoContainer.getChildren().setAll(iv);
        } else {
            brandLogoContainer.getChildren().setAll(AliBrandLogo.build(32));
        }
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
                scaleTransition.setToX(0.97);
                scaleTransition.setToY(0.97);
                scaleTransition.setAutoReverse(true);
                scaleTransition.setCycleCount(2);
                scaleTransition.play();
            });
        });
    }

    private void applyNavStyle(Button btn, boolean active) {
        btn.getStyleClass().removeAll(NAV_STYLE, "nav-tab-btn-active");
        btn.getStyleClass().add(NAV_STYLE);
        if (active) {
            btn.getStyleClass().add("nav-tab-btn-active");
        }
    }

    private void setActiveNavButton(Button button) {
        navMap.forEach((btn, page) -> {
            applyNavStyle(btn, false);
            page.setVisible(false);
        });
        activeNavButton = button;
        applyNavStyle(button, true);
        navMap.get(button).setVisible(true);
    }

    private void setupWindowControls() {
        if (minimizeBtn == null || maximizeBtn == null || closeBtn == null) {
            return;
        }

        minimizeBtn.setDefaultButton(false);
        maximizeBtn.setDefaultButton(false);
        closeBtn.setDefaultButton(false);
        minimizeBtn.setCancelButton(false);
        maximizeBtn.setCancelButton(false);
        closeBtn.setCancelButton(false);
        minimizeBtn.setFocusTraversable(false);
        maximizeBtn.setFocusTraversable(false);
        closeBtn.setFocusTraversable(false);

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
    }

    private static boolean isUnderButton(Node n) {
        while (n != null) {
            if (n instanceof Button) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    private void setupTopChromeDrag() {
        topChrome.setOnMousePressed(event -> {
            if (isUnderButton(event.getTarget() instanceof Node ? (Node) event.getTarget() : null)) {
                return;
            }
            Stage stage = (Stage) topChrome.getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        topChrome.setOnMouseDragged(event -> {
            if (isUnderButton(event.getTarget() instanceof Node ? (Node) event.getTarget() : null)) {
                return;
            }
            Stage stage = (Stage) topChrome.getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
    }
}
