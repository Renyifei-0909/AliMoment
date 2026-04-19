package com.aiimoment.account;

import com.aiimoment.controller.SettingsPageController;
import com.aiimoment.ui.AlimomentDialogs;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 设置页「账户管理」卡片：头像弹层、账户下拉、持久化与切换回调。
 */
public final class AccountSectionBinder {

    private static final List<String> AVATAR_EXTENSIONS = Arrays.asList("*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp");

    private final SettingsPageController settingsPage;
    private final StackPane avatarHitbox;
    private final ImageView avatarImage;
    private final Label avatarInitialLabel;
    private final Label summaryNameLabel;
    private final Label summaryEmailLabel;
    private final StackPane menuHitbox;

    private final Popup avatarFlyout = new Popup();
    private final Popup accountMenuPopup = new Popup();

    private final ImageView flyoutAvatarImage = new ImageView();
    private final Label flyoutAvatarInitial = new Label();
    private final Label flyoutNameLabel = new Label();
    private final Label flyoutEmailLabel = new Label();
    private final Hyperlink changeAvatarLink = new Hyperlink("修改头像");
    private final Hyperlink changeNameLink = new Hyperlink("修改用户名");
    private final HBox nameEditRow = new HBox(6);
    private final TextField nameEditField = new TextField();
    private final Button nameConfirmBtn = new Button("✓");
    private final Button nameCancelBtn = new Button("✕");
    private boolean nameEditing;

    private EventHandlerRegistration flyoutEsc;
    private EventHandlerRegistration menuEsc;

    public AccountSectionBinder(
            SettingsPageController settingsPage,
            StackPane avatarHitbox,
            ImageView avatarImage,
            Label avatarInitialLabel,
            Label summaryNameLabel,
            Label summaryEmailLabel,
            StackPane menuHitbox) {
        this.settingsPage = settingsPage;
        this.avatarHitbox = avatarHitbox;
        this.avatarImage = avatarImage;
        this.avatarInitialLabel = avatarInitialLabel;
        this.summaryNameLabel = summaryNameLabel;
        this.summaryEmailLabel = summaryEmailLabel;
        this.menuHitbox = menuHitbox;
    }

    public void install() {
        AccountStore.getInstance().load();
        AccountStore.getInstance().persistIfNew();

        setupAvatarClip(avatarHitbox, avatarImage, 40);
        StackPane.setAlignment(avatarImage, Pos.CENTER);
        StackPane.setAlignment(avatarInitialLabel, Pos.CENTER);

        flyoutAvatarImage.setPreserveRatio(true);
        flyoutAvatarImage.setSmooth(true);
        flyoutAvatarImage.setFitWidth(56);
        flyoutAvatarImage.setFitHeight(56);

        avatarHitbox.setCursor(javafx.scene.Cursor.HAND);
        menuHitbox.setCursor(javafx.scene.Cursor.HAND);

        buildAvatarFlyout();
        buildAccountMenuShell();

        accountMenuPopup.showingProperty().addListener((o, ov, nv) -> {
            if (menuHitbox == null) {
                return;
            }
            if (Boolean.TRUE.equals(nv)) {
                menuHitbox.getStyleClass().add("settings-account-menu-open");
            } else {
                menuHitbox.getStyleClass().remove("settings-account-menu-open");
            }
        });

        avatarHitbox.setOnMouseClicked(e -> {
            hideAccountMenu();
            toggleAvatarFlyout();
            e.consume();
        });
        menuHitbox.setOnMouseClicked(e -> {
            hideAvatarFlyout();
            toggleAccountMenu();
            e.consume();
        });

        refreshAll();
    }

    private static void setupAvatarClip(StackPane hitbox, ImageView imageView, double diameter) {
        Circle clip = new Circle(diameter / 2, diameter / 2, diameter / 2);
        hitbox.setClip(clip);
        hitbox.setMinSize(diameter, diameter);
        hitbox.setPrefSize(diameter, diameter);
        hitbox.setMaxSize(diameter, diameter);
        if (imageView != null) {
            imageView.setFitWidth(diameter);
            imageView.setFitHeight(diameter);
            StackPane.setAlignment(imageView, Pos.CENTER);
        }
    }

    private void buildAvatarFlyout() {
        flyoutAvatarInitial.getStyleClass().add("account-flyout-initial");
        StackPane flyAvatarStack = new StackPane();
        flyAvatarStack.getChildren().addAll(flyoutAvatarImage, flyoutAvatarInitial);
        flyAvatarStack.setMinSize(56, 56);
        flyAvatarStack.setPrefSize(56, 56);
        flyAvatarStack.setMaxSize(56, 56);
        Circle c = new Circle(28, 28, 28);
        flyAvatarStack.setClip(c);

        flyoutNameLabel.getStyleClass().add("account-flyout-name");
        flyoutEmailLabel.getStyleClass().add("account-flyout-email");
        changeAvatarLink.getStyleClass().add("account-flyout-action");
        changeNameLink.getStyleClass().add("account-flyout-action");

        nameEditField.getStyleClass().add("account-flyout-name-field");
        nameEditField.setPromptText("用户名");
        nameConfirmBtn.getStyleClass().addAll("account-flyout-icon-btn", "account-flyout-confirm");
        nameCancelBtn.getStyleClass().addAll("account-flyout-icon-btn", "account-flyout-cancel");
        nameEditRow.setAlignment(Pos.CENTER_LEFT);
        nameEditRow.getChildren().addAll(nameEditField, nameConfirmBtn, nameCancelBtn);
        HBox.setHgrow(nameEditField, Priority.ALWAYS);
        nameEditRow.setVisible(false);
        nameEditRow.managedProperty().bind(nameEditRow.visibleProperty());

        VBox body = new VBox(10);
        body.setPadding(new Insets(12, 14, 12, 14));
        body.getStyleClass().add("account-flyout-body");

        Polygon arrow = new Polygon(0.0, 8.0, 8.0, 0.0, 16.0, 8.0);
        arrow.getStyleClass().add("account-flyout-arrow");

        VBox root = new VBox();
        root.getStyleClass().add("account-flyout-root");
        StackPane arrowWrap = new StackPane(arrow);
        arrowWrap.setPadding(new Insets(0, 0, -1, 0));
        root.getChildren().addAll(arrowWrap, body);

        body.getChildren().addAll(
                flyAvatarStack,
                flyoutNameLabel,
                flyoutEmailLabel,
                nameEditRow,
                changeAvatarLink,
                changeNameLink
        );

        avatarFlyout.getContent().setAll(root);
        avatarFlyout.setAutoHide(true);
        avatarFlyout.setOnHidden(e -> {
            if (flyoutEsc != null) {
                flyoutEsc.remove();
                flyoutEsc = null;
            }
        });

        changeAvatarLink.setOnAction(e -> pickAndApplyAvatar());
        changeNameLink.setOnAction(e -> enterNameEditMode());

        Runnable confirmName = this::confirmNameEdit;
        Runnable cancelName = this::cancelNameEdit;
        nameConfirmBtn.setOnAction(e -> confirmName.run());
        nameCancelBtn.setOnAction(e -> cancelName.run());
        nameEditField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                confirmName.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                cancelName.run();
                e.consume();
            }
        });
    }

    private void buildAccountMenuShell() {
        VBox root = new VBox(0);
        root.getStyleClass().add("account-menu-root");
        root.setPadding(new Insets(8, 0, 8, 0));
        root.setMinWidth(260);
        accountMenuPopup.getContent().setAll(root);
        accountMenuPopup.setAutoHide(true);
        accountMenuPopup.setOnHidden(e -> {
            if (menuEsc != null) {
                menuEsc.remove();
                menuEsc = null;
            }
        });
    }

    private void attachEsc(Popup popup, Runnable onEsc) {
        Platform.runLater(() -> {
            Scene sc = popup.getScene();
            if (sc == null) {
                return;
            }
            EventHandler<KeyEvent> escHandler = e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    popup.hide();
                    if (onEsc != null) {
                        onEsc.run();
                    }
                    e.consume();
                }
            };
            sc.addEventFilter(KeyEvent.KEY_PRESSED, escHandler);
            if (popup == avatarFlyout) {
                if (flyoutEsc != null) {
                    flyoutEsc.remove();
                }
                flyoutEsc = new EventHandlerRegistration(sc, escHandler);
            } else {
                if (menuEsc != null) {
                    menuEsc.remove();
                }
                menuEsc = new EventHandlerRegistration(sc, escHandler);
            }
        });
    }

    private static final class EventHandlerRegistration {
        private final Scene scene;
        private final EventHandler<KeyEvent> handler;

        EventHandlerRegistration(Scene scene, EventHandler<KeyEvent> handler) {
            this.scene = scene;
            this.handler = handler;
        }

        void remove() {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
        }
    }

    private void toggleAvatarFlyout() {
        if (avatarFlyout.isShowing()) {
            avatarFlyout.hide();
            return;
        }
        syncFlyoutContent();
        positionPopupBelow(avatarFlyout, avatarHitbox, true);
        avatarFlyout.show(avatarHitbox, screenX(avatarHitbox), screenBelowY(avatarHitbox));
        attachEsc(avatarFlyout, this::cancelNameEditSilently);
    }

    private void hideAvatarFlyout() {
        avatarFlyout.hide();
    }

    private void toggleAccountMenu() {
        if (accountMenuPopup.isShowing()) {
            accountMenuPopup.hide();
            return;
        }
        rebuildAccountMenuContent();
        positionPopupBelow(accountMenuPopup, menuHitbox, false);
        accountMenuPopup.show(menuHitbox, screenX(menuHitbox), screenBelowY(menuHitbox));
        attachEsc(accountMenuPopup, null);
    }

    private void hideAccountMenu() {
        accountMenuPopup.hide();
    }

    private static double screenX(Region anchor) {
        return anchor.localToScreen(0, 0).getX();
    }

    private static double screenBelowY(Region anchor) {
        return anchor.localToScreen(0, anchor.getHeight()).getY() + 4;
    }

    private void positionPopupBelow(Popup popup, Region anchor, boolean centerArrowOnAvatar) {
        Platform.runLater(() -> {
            javafx.scene.Node content = popup.getContent().isEmpty() ? null : popup.getContent().get(0);
            if (!(content instanceof VBox)) {
                return;
            }
            VBox root = (VBox) content;
            double pw = Math.max(260, anchor.getScene() != null ? anchor.getScene().getWidth() * 0.35 : 280);
            root.setMinWidth(pw);
            /* 粗略将箭头对齐头像中心 */
            if (centerArrowOnAvatar && root.getChildren().size() > 0) {
                javafx.scene.Node first = root.getChildren().get(0);
                if (first instanceof StackPane) {
                    StackPane arrowWrap = (StackPane) first;
                    double ax = anchor.localToScreen(anchor.getWidth() / 2, 0).getX();
                    double px = anchor.localToScreen(0, 0).getX();
                    double pad = Math.max(12, Math.min(pw / 2 - 16, ax - px - 8));
                    arrowWrap.setPadding(new Insets(0, 0, 0, pad));
                }
            }
        });
    }

    private void syncFlyoutContent() {
        AccountRecord cur = AccountStore.getInstance().getCurrent();
        flyoutNameLabel.setText(cur.getDisplayName());
        flyoutEmailLabel.setText(cur.getEmail().isEmpty() ? "未绑定邮箱" : cur.getEmail());
        applyAvatarImage(flyoutAvatarImage, flyoutAvatarInitial, cur.getAvatarPath(), cur.getDisplayName(), 56);
        cancelNameEditSilently();
    }

    private void enterNameEditMode() {
        nameEditing = true;
        flyoutNameLabel.setVisible(false);
        flyoutNameLabel.setManaged(false);
        nameEditRow.setVisible(true);
        nameEditField.setText(AccountStore.getInstance().getCurrent().getDisplayName());
        Platform.runLater(() -> {
            nameEditField.requestFocus();
            nameEditField.selectAll();
        });
    }

    private void confirmNameEdit() {
        String v = nameEditField.getText() != null ? nameEditField.getText().trim() : "";
        if (v.isEmpty()) {
            Window w = ownerWindow();
            if (w != null) {
                AlimomentDialogs.showError(w, "用户名无效", "用户名不能为空。");
            }
            return;
        }
        AccountStore.getInstance().updateCurrentProfile(v, null);
        AccountStore.getInstance().save();
        cancelNameEditSilently();
        syncFlyoutContent();
        refreshSummary();
    }

    private void cancelNameEdit() {
        cancelNameEditSilently();
    }

    private void cancelNameEditSilently() {
        nameEditing = false;
        nameEditRow.setVisible(false);
        flyoutNameLabel.setVisible(true);
        flyoutNameLabel.setManaged(true);
    }

    private void pickAndApplyAvatar() {
        Window owner = ownerWindow();
        if (owner == null) {
            return;
        }
        FileChooser ch = new FileChooser();
        ch.setTitle("选择头像图片");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", AVATAR_EXTENSIONS));
        File f = ch.showOpenDialog(owner);
        if (f == null) {
            return;
        }
        String path = AccountStore.getInstance().importAvatarFile(AccountStore.getInstance().getCurrentAccountId(), f.toPath());
        if (path == null) {
            AlimomentDialogs.showError(owner, "导入失败", "无法保存头像文件，请重试或更换路径。");
            return;
        }
        AccountStore.getInstance().updateCurrentAvatarPath(path);
        AccountStore.getInstance().save();
        syncFlyoutContent();
        refreshSummary();
    }

    private Window ownerWindow() {
        Scene sc = avatarHitbox.getScene();
        return sc != null ? sc.getWindow() : null;
    }

    private void rebuildAccountMenuContent() {
        VBox root = (VBox) accountMenuPopup.getContent().get(0);
        root.getChildren().clear();

        List<AccountRecord> list = new ArrayList<>(AccountStore.getInstance().listAccounts());
        String currentId = AccountStore.getInstance().getCurrentAccountId();

        for (AccountRecord a : list) {
            root.getChildren().add(buildAccountMenuRow(a, currentId));
        }
        root.getChildren().add(new Separator());
        root.getChildren().add(buildAddAccountRow());
    }

    private Region buildAccountMenuRow(AccountRecord a, String currentId) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 12, 6, 12));
        row.getStyleClass().add("account-menu-row");
        if (a.getId().equals(currentId)) {
            row.getStyleClass().add("account-menu-row-current");
        }

        ImageView mini = new ImageView();
        mini.setFitWidth(28);
        mini.setFitHeight(28);
        Label ini = new Label();
        ini.getStyleClass().add("account-menu-mini-initial");
        StackPane av = new StackPane(mini, ini);
        av.setMinSize(28, 28);
        av.setPrefSize(28, 28);
        Circle clip = new Circle(14, 14, 14);
        av.setClip(clip);
        applyAvatarImage(mini, ini, a.getAvatarPath(), a.getDisplayName(), 28);

        VBox text = new VBox(2);
        Label n = new Label(a.getDisplayName());
        n.getStyleClass().add("account-menu-name");
        Label em = new Label(a.getEmail().isEmpty() ? "—" : a.getEmail());
        em.getStyleClass().add("account-menu-email");
        text.getChildren().addAll(n, em);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label mark = new Label(a.getId().equals(currentId) ? "当前" : "");
        mark.getStyleClass().add("account-menu-current-mark");

        row.getChildren().addAll(av, text, mark);

        if (!a.getId().equals(currentId)) {
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(ev -> confirmAndSwitchTo(a));
        }
        return row;
    }

    private void confirmAndSwitchTo(AccountRecord target) {
        Window w = ownerWindow();
        if (w == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(w);
        alert.setTitle("切换账户");
        alert.setHeaderText(null);
        alert.setContentText("切换到账户「" + target.getDisplayName() + "」？");
        Optional<ButtonType> r = alert.showAndWait();
        if (!r.isPresent() || r.get() != ButtonType.OK) {
            return;
        }
        AccountStore.getInstance().setCurrentAccountId(target.getId());
        accountMenuPopup.hide();
        settingsPage.reloadPerAccountSettingsInUi();
        refreshAll();
    }

    private Region buildAddAccountRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("account-menu-add-row");
        row.setCursor(javafx.scene.Cursor.HAND);

        StackPane plusWrap = new StackPane();
        plusWrap.setMinSize(28, 28);
        plusWrap.setPrefSize(28, 28);
        plusWrap.getStyleClass().add("account-menu-add-icon-wrap");
        Label plus = new Label("+");
        plus.getStyleClass().add("account-menu-add-plus");
        plusWrap.getChildren().add(plus);

        Label txt = new Label("添加新账号");
        txt.getStyleClass().add("account-menu-add-text");
        row.getChildren().addAll(plusWrap, txt);
        row.setOnMouseClicked(e -> openAddAccountDialog());
        return row;
    }

    private void openAddAccountDialog() {
        Window w = ownerWindow();
        if (w == null) {
            return;
        }
        accountMenuPopup.hide();

        TextField nameField = new TextField();
        nameField.setPromptText("用户名");
        TextField emailField = new TextField();
        emailField.setPromptText("邮箱（可选）");

        VBox form = new VBox(10, new Label("登录 / 注册（占位）"), nameField, emailField);
        form.setPadding(new Insets(16));

        Alert dialog = new Alert(Alert.AlertType.NONE);
        dialog.initOwner(w);
        dialog.setTitle("添加新账号");
        dialog.setHeaderText("完成基本信息后即可加入账户列表");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> res = dialog.showAndWait();
        if (!res.isPresent() || res.get() != ButtonType.OK) {
            return;
        }
        String nm = nameField.getText() != null ? nameField.getText().trim() : "";
        if (nm.isEmpty()) {
            AlimomentDialogs.showError(w, "无法添加", "请填写用户名。");
            return;
        }
        String em = emailField.getText() != null ? emailField.getText().trim() : "";
        AccountStore.getInstance().addAccount(nm, em);
        settingsPage.reloadPerAccountSettingsInUi();
        refreshAll();
    }

    public void refreshAll() {
        refreshSummary();
        if (avatarFlyout.isShowing()) {
            syncFlyoutContent();
        }
    }

    private void refreshSummary() {
        AccountRecord cur = AccountStore.getInstance().getCurrent();
        summaryNameLabel.setText(cur.getDisplayName());
        summaryEmailLabel.setText(cur.getEmail().isEmpty() ? "未绑定邮箱" : cur.getEmail());
        applyAvatarImage(avatarImage, avatarInitialLabel, cur.getAvatarPath(), cur.getDisplayName(), 40);
    }

    private static void applyAvatarImage(ImageView iv, Label initial, String path, String displayName, double size) {
        boolean shown = false;
        if (path != null && !path.isEmpty()) {
            try {
                File f = new File(path);
                if (f.isFile()) {
                    Image img = new Image(f.toURI().toString(), size, size, true, true, true);
                    if (!img.isError()) {
                        iv.setImage(img);
                        iv.setVisible(true);
                        iv.setManaged(true);
                        initial.setVisible(false);
                        initial.setManaged(false);
                        shown = true;
                    }
                }
            } catch (Exception ignored) {
                /* fall through to initial */
            }
        }
        if (!shown) {
            iv.setImage(null);
            iv.setVisible(false);
            iv.setManaged(false);
            initial.setVisible(true);
            initial.setManaged(true);
            String dn = displayName != null ? displayName.trim() : "";
            String ch = dn.isEmpty() ? "?" : dn.substring(0, 1);
            initial.setText(ch);
        }
    }
}
