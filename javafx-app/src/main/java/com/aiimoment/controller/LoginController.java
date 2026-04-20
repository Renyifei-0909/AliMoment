package com.aiimoment.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;

public class LoginController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberCheck;
    @FXML
    private Button loginButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label twinkleStar;
    @FXML
    private Label starA;
    @FXML
    private Label starB;
    @FXML
    private Label starC;
    @FXML
    private Label starD;
    @FXML
    private Label starE;
    @FXML
    private Label starF;
    @FXML
    private Label starG;
    @FXML
    private Label starH;
    @FXML
    private Label starI;
    @FXML
    private Button loginMinimizeBtn;
    @FXML
    private Button loginMaximizeBtn;
    @FXML
    private Button loginCloseBtn;

    private Runnable onLoginSuccess = () -> {};

    @FXML
    public void initialize() {
        hideError();
        setupStarTwinkleAnimation();
        setupWindowControls();
        if (passwordField != null) {
            passwordField.setOnAction(e -> onLogin());
        }
        if (emailField != null) {
            emailField.setOnAction(e -> onLogin());
        }
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess == null ? () -> {} : onLoginSuccess;
    }

    @FXML
    public void onLogin() {
        String email = emailField == null ? "" : emailField.getText().trim();
        String password = passwordField == null ? "" : passwordField.getText();

        if (email.isEmpty()) {
            showError("请输入邮箱");
            return;
        }
        if (password.isEmpty()) {
            showError("请输入密码");
            return;
        }

        // 先使用本地通过逻辑占位，后续可直接替换为真实后端鉴权请求。
        hideError();
        loginButton.setDisable(true);
        loginButton.setText("登录中...");

        javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(Duration.millis(520));
        wait.setOnFinished(e -> {
            loginButton.setDisable(false);
            loginButton.setText("登录");
            onLoginSuccess.run();
        });
        wait.play();
    }

    private void setupStarTwinkleAnimation() {
        List<Label> stars = Arrays.asList(twinkleStar, starA, starB, starC, starD, starE, starF, starG, starH, starI);
        int i = 0;
        for (Label star : stars) {
            if (star == null) {
                continue;
            }
            double from = 0.18 + (i % 3) * 0.05;
            double to = 0.78 + (i % 2) * 0.16;
            double fromScale = 0.88 + (i % 2) * 0.05;
            double toScale = 1.02 + (i % 3) * 0.06;
            int inMs = 1700 + i * 220;
            int outMs = 2200 + i * 180;

            FadeTransition fadeIn = new FadeTransition(Duration.millis(inMs), star);
            fadeIn.setFromValue(from);
            fadeIn.setToValue(to);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(outMs), star);
            fadeOut.setFromValue(to);
            fadeOut.setToValue(from);

            ScaleTransition grow = new ScaleTransition(Duration.millis(inMs), star);
            grow.setFromX(fromScale);
            grow.setFromY(fromScale);
            grow.setToX(toScale);
            grow.setToY(toScale);

            ScaleTransition shrink = new ScaleTransition(Duration.millis(outMs), star);
            shrink.setFromX(toScale);
            shrink.setFromY(toScale);
            shrink.setToX(fromScale);
            shrink.setToY(fromScale);

            SequentialTransition seq = new SequentialTransition(
                    new ParallelTransition(fadeIn, grow),
                    new ParallelTransition(fadeOut, shrink)
            );
            seq.setCycleCount(Timeline.INDEFINITE);
            seq.setDelay(Duration.millis(i * 210L));
            seq.play();
            i++;
        }
    }

    private void setupWindowControls() {
        if (loginMinimizeBtn == null || loginMaximizeBtn == null || loginCloseBtn == null) {
            return;
        }
        loginMinimizeBtn.setOnAction(e -> {
            Stage stage = (Stage) loginMinimizeBtn.getScene().getWindow();
            stage.setIconified(true);
        });
        loginMaximizeBtn.setOnAction(e -> {
            Stage stage = (Stage) loginMaximizeBtn.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
        loginCloseBtn.setOnAction(e -> {
            Stage stage = (Stage) loginCloseBtn.getScene().getWindow();
            stage.close();
        });
    }

    private void showError(String msg) {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
