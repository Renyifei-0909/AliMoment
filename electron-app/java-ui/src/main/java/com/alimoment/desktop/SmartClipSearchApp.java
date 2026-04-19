package com.alimoment.desktop;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SmartClipSearchApp extends Application {

    private final ObservableList<SearchResult> allResults = FXCollections.observableArrayList(
            new SearchResult("00:01:23-00:01:35", 0.62, "小女孩在公园中央的草地上快乐的奔跑，阳光洒在她脸上。"),
            new SearchResult("00:02:45-00:03:10", 0.87, "小女孩穿着粉色裙子在喷泉旁旋转跳舞"),
            new SearchResult("00:03:50-00:04:15", 0.76, "小女孩和小狗在沙坑里一起玩耍的画面")
    );

    private final VBox resultListContainer = new VBox(16);
    private final Label emptyLabel = new Label("没有找到相关视频片段");
    private TextArea searchField;

    private StackPane centerContainer;
    private List<VBox> navButtons = new ArrayList<>();

    // 设置界面右侧滚动容器
    private ScrollPane settingsScrollPane;
    // 右侧统一内容容器
    private VBox allSettingsContainer;

    // 主题相关
    private Scene appScene;
    private BorderPane rootPane;
    private String currentTheme = "dark"; // dark, light, auto

    @Override
    public void start(Stage stage) {
        rootPane = new BorderPane();
        rootPane.getStyleClass().add("app-root");
        rootPane.setPadding(new Insets(0, 0, 0, 0));

        VBox topArea = new VBox();
        topArea.getChildren().addAll(buildBrandBar(), buildTopBar());
        rootPane.setTop(topArea);
        rootPane.setLeft(buildRail());

        centerContainer = new StackPane();
        centerContainer.setAlignment(Pos.TOP_CENTER);
        rootPane.setCenter(centerContainer);
        switchToSmartSearch();

        appScene = new Scene(rootPane, 1600, 920);
        applyCss(appScene);

        stage.setTitle("Alimoment - 智能检索");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.setScene(appScene);
        stage.show();

        filterResults("");
        // 初始化主题（默认深色）
        applyTheme("dark");
    }

    private void switchToSmartSearch() {
        VBox smartView = buildSmartSearchView();
        centerContainer.getChildren().setAll(smartView);
    }

    private void switchToBlank(String title) {
        VBox blankView = buildBlankView(title);
        centerContainer.getChildren().setAll(blankView);
    }

    private void switchToSettings() {
        VBox settingsView = buildSettingsView();
        centerContainer.getChildren().setAll(settingsView);
    }

    private VBox buildSmartSearchView() {
        HBox workspace = new HBox(16);
        workspace.setMaxWidth(Double.MAX_VALUE);
        VBox leftStudio = buildStudioPane();
        VBox rightSearchPanel = buildSearchPanel();
        HBox.setHgrow(leftStudio, Priority.ALWAYS);
        workspace.getChildren().addAll(leftStudio, rightSearchPanel);

        StackPane workspaceWrap = new StackPane(workspace);
        workspaceWrap.setAlignment(Pos.TOP_CENTER);
        workspaceWrap.setPadding(new Insets(0, 16, 0, 16));

        VBox main = new VBox(10);
        main.getStyleClass().add("main-content");
        main.setPadding(new Insets(0, 0, 12, 0));
        main.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(workspaceWrap, Priority.ALWAYS);
        main.getChildren().add(workspaceWrap);
        return main;
    }

    private VBox buildBlankView(String title) {
        VBox blank = new VBox();
        blank.getStyleClass().add("blank-view");
        blank.setAlignment(Pos.CENTER);
        Label label = new Label(title + "界面（空白）");
        label.getStyleClass().add("blank-label");
        blank.getChildren().add(label);
        return blank;
    }

    // ======================= 设置界面 =======================
    private VBox buildSettingsView() {
        VBox root = new VBox();
        root.getStyleClass().add("settings-container");
        root.setPadding(new Insets(0, 16, 16, 16));

        HBox mainArea = new HBox(20);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        // 左侧树形功能列表
        VBox sidebar = buildSettingsSidebar();
        sidebar.setPrefWidth(260);
        sidebar.setMaxWidth(260);

        // 右侧内容滚动区
        settingsScrollPane = new ScrollPane();
        settingsScrollPane.setFitToWidth(true);
        settingsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        settingsScrollPane.getStyleClass().add("result-scroll");

        // 构建统一内容容器
        allSettingsContainer = buildAllSettingsContent();
        settingsScrollPane.setContent(allSettingsContainer);

        mainArea.getChildren().addAll(sidebar, settingsScrollPane);
        HBox.setHgrow(settingsScrollPane, Priority.ALWAYS);

        HBox bottomBar = buildBottomStatsBar();

        root.getChildren().addAll(mainArea, bottomBar);
        return root;
    }

    // 构建统一的所有设置内容
    private VBox buildAllSettingsContent() {
        VBox container = new VBox(20);
        container.getStyleClass().add("settings-content-area");

        // 通用设置分组
        container.getChildren().add(createSettingsGroupWithId("theme-group", buildThemeAppearanceContent()));
        container.getChildren().add(createSettingsGroupWithId("language-group", buildLanguageRegionContent()));
        container.getChildren().add(createSettingsGroupWithId("account-group", buildAccountManagementContent()));
        container.getChildren().add(createSettingsGroupWithId("shortcut-group", buildShortcutContent()));
        container.getChildren().add(createSettingsGroupWithId("cache-group", buildCacheManagementContent()));

        // AI功能设置分组
        container.getChildren().add(createSettingsGroupWithId("ai-switch-group", buildAISwitchContent()));
        container.getChildren().add(createSettingsGroupWithId("auto-clip-group", buildAutoClipContent()));
        container.getChildren().add(createSettingsGroupWithId("smart-subtitle-group", buildSmartSubtitleContent()));
        container.getChildren().add(createSettingsGroupWithId("ai-weight-group", buildAIDetectionWeightContent()));
        container.getChildren().add(createSettingsGroupWithId("voice-audio-group", buildVoiceAudioContent()));

        // 项目与导出分组
        container.getChildren().add(createSettingsGroupWithId("save-path-group", buildSavePathContent()));
        container.getChildren().add(createSettingsGroupWithId("auto-save-group", buildAutoSaveContent()));
        container.getChildren().add(createSettingsGroupWithId("default-export-group", buildDefaultExportContent()));

        return container;
    }

    // 为设置分组包装一个 VBox，并设置 id
    private VBox createSettingsGroupWithId(String id, VBox content) {
        VBox group = new VBox();
        group.setId(id);
        group.getChildren().add(content);
        return group;
    }

    // 滚动到指定分组
    private void scrollToGroup(String key) {
        if (settingsScrollPane == null || allSettingsContainer == null) return;
        String id = null;
        switch (key) {
            case "theme":
                id = "theme-group";
                break;
            case "language":
                id = "language-group";
                break;
            case "account":
                id = "account-group";
                break;
            case "shortcut":
                id = "shortcut-group";
                break;
            case "cache":
                id = "cache-group";
                break;
            case "ai_switch":
                id = "ai-switch-group";
                break;
            case "auto_clip":
                id = "auto-clip-group";
                break;
            case "smart_subtitle":
                id = "smart-subtitle-group";
                break;
            case "ai_weight":
                id = "ai-weight-group";
                break;
            case "voice_audio":
                id = "voice-audio-group";
                break;
            case "save_path":
                id = "save-path-group";
                break;
            case "auto_save":
                id = "auto-save-group";
                break;
            case "default_export":
                id = "default-export-group";
                break;
            default:
                return;
        }
        Node target = allSettingsContainer.lookup("#" + id);
        if (target != null) {
            double y = target.getBoundsInParent().getMinY();
            double contentHeight = allSettingsContainer.getHeight();
            double viewportHeight = settingsScrollPane.getViewportBounds().getHeight();
            if (contentHeight > viewportHeight) {
                double vvalue = y / (contentHeight - viewportHeight);
                settingsScrollPane.setVvalue(Math.min(1.0, Math.max(0.0, vvalue)));
            }
        }
    }

    // 左侧树形列表
    private VBox buildSettingsSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("settings-sidebar");

        Label generalGroup = new Label("通用设置");
        generalGroup.getStyleClass().add("settings-group-title");
        generalGroup.setPadding(new Insets(8, 0, 4, 0));
        VBox.setMargin(generalGroup, new Insets(0, 0, 0, 0));

        VBox generalItems = new VBox(4);
        generalItems.setPadding(new Insets(0, 0, 0, 16));
        generalItems.getChildren().addAll(
                createSidebarItem("主题与外观", "theme"),
                createSidebarItem("语言与地区", "language"),
                createSidebarItem("账户管理", "account"),
                createSidebarItem("快捷键", "shortcut"),
                createSidebarItem("缓存管理", "cache")
        );

        Label aiGroup = new Label("AI功能设置");
        aiGroup.getStyleClass().add("settings-group-title");
        aiGroup.setPadding(new Insets(12, 0, 4, 0));

        VBox aiItems = new VBox(4);
        aiItems.setPadding(new Insets(0, 0, 0, 16));
        aiItems.getChildren().addAll(
                createSidebarItem("AI辅助开关", "ai_switch"),
                createSidebarItem("自动剪辑", "auto_clip"),
                createSidebarItem("智能字幕", "smart_subtitle"),
                createSidebarItem("AI检测权重", "ai_weight"),
                createSidebarItem("语音与音频", "voice_audio")
        );

        Label projectGroup = new Label("项目与导出");
        projectGroup.getStyleClass().add("settings-group-title");
        projectGroup.setPadding(new Insets(12, 0, 4, 0));

        VBox projectItems = new VBox(4);
        projectItems.setPadding(new Insets(0, 0, 0, 32));
        projectItems.getChildren().addAll(
                createSidebarItem("保存路径", "save_path"),
                createSidebarItem("自动保存", "auto_save"),
                createSidebarItem("默认导出设置", "default_export")
        );

        sidebar.getChildren().addAll(
                generalGroup, generalItems,
                aiGroup, aiItems,
                projectGroup, projectItems
        );
        return sidebar;
    }

    private Label createSidebarItem(String text, String key) {
        Label item = new Label(text);
        item.getStyleClass().add("settings-category-item");
        item.setMaxWidth(Double.MAX_VALUE);
        item.setUserData(key);
        item.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            scrollToGroup(key);
        });
        return item;
    }

    // ---------- 各设置面板构建方法 ----------
    private VBox buildThemeAppearanceContent() {
        VBox group = createSettingsGroup("主题与外观");

        // 创建三个主题卡片容器
        HBox cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER_LEFT);
        cardsContainer.setPadding(new Insets(10, 0, 10, 0));

        // 深色卡片
        VBox darkCard = createThemeCard("深色", "#1C1C1E", "#111113", "#2A2A30");
        // 浅色卡片
        VBox lightCard = createThemeCard("浅色", "#E5E5EA", "#FFFFFF", "#F2F2F6");
        // 自动卡片
        VBox autoCard = createThemeCard("跟随系统", "#808080", "#4A4A4E", "#C0C0C4");

        // 设置初始选中状态（深色选中）
        setActiveCard(darkCard);
        setInactiveCard(lightCard);
        setInactiveCard(autoCard);

        // 添加点击事件
        darkCard.setOnMouseClicked(e -> {
            setActiveCard(darkCard);
            setInactiveCard(lightCard);
            setInactiveCard(autoCard);
            applyTheme("dark");
        });
        lightCard.setOnMouseClicked(e -> {
            setActiveCard(lightCard);
            setInactiveCard(darkCard);
            setInactiveCard(autoCard);
            applyTheme("light");
        });
        autoCard.setOnMouseClicked(e -> {
            setActiveCard(autoCard);
            setInactiveCard(darkCard);
            setInactiveCard(lightCard);
            applyTheme("auto");
        });

        cardsContainer.getChildren().addAll(darkCard, lightCard, autoCard);
        group.getChildren().add(cardsContainer);
        return group;
    }

    // 创建主题卡片（包含颜色预览区和文字）
    private VBox createThemeCard(String title, String primaryColor, String secondaryColor, String accentColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(120);
        card.setPrefHeight(110);
        card.getStyleClass().add("theme-card");

        // 颜色预览区（两个色块模拟主题预览）
        HBox previewArea = new HBox(2);
        previewArea.setAlignment(Pos.CENTER);
        previewArea.setPrefHeight(60);
        Region leftBlock = new Region();
        leftBlock.setPrefSize(50, 50);
        leftBlock.setStyle("-fx-background-color: " + primaryColor + "; -fx-background-radius: 8 0 0 8;");
        Region rightBlock = new Region();
        rightBlock.setPrefSize(50, 50);
        rightBlock.setStyle("-fx-background-color: " + secondaryColor + "; -fx-background-radius: 0 8 8 0;");
        previewArea.getChildren().addAll(leftBlock, rightBlock);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("theme-card-title");

        card.getChildren().addAll(previewArea, titleLabel);
        return card;
    }

    private void setActiveCard(VBox card) {
        card.getStyleClass().add("theme-card-active");
        card.getStyleClass().remove("theme-card");
    }

    private void setInactiveCard(VBox card) {
        card.getStyleClass().add("theme-card");
        card.getStyleClass().remove("theme-card-active");
    }

    // 主题应用逻辑
    private void applyTheme(String theme) {
        currentTheme = theme;
        if ("auto".equals(theme)) {
            // 简单模拟：根据系统时间（18点-6点为深色，否则浅色）
            LocalTime now = LocalTime.now();
            boolean isDark = now.isAfter(LocalTime.of(18, 0)) || now.isBefore(LocalTime.of(6, 0));
            applyThemeColors(isDark);
        } else if ("dark".equals(theme)) {
            applyThemeColors(true);
        } else {
            applyThemeColors(false);
        }
    }

    private void applyThemeColors(boolean isDark) {
        if (isDark) {
            rootPane.getStyleClass().remove("light-theme");
        } else {
            if (!rootPane.getStyleClass().contains("light-theme")) {
                rootPane.getStyleClass().add("light-theme");
            }
        }
        // 强制刷新样式表（解决某些情况下样式未更新的问题）
        if (appScene != null) {
            appScene.getStylesheets().setAll(getClass().getResource("/styles.css").toExternalForm());
        }
    }

    private VBox buildLanguageRegionContent() {
        VBox group = createSettingsGroup("语言与地区");
        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("简体中文", "English", "日本語");
        langCombo.setValue("简体中文");
        langCombo.getStyleClass().add("setting-control");
        group.getChildren().add(langCombo);
        return group;
    }

    private VBox buildAccountManagementContent() {
        VBox group = createSettingsGroup("账户管理");
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Label email = new Label("user@alimoment.com");
        email.getStyleClass().add("account-email");
        Button modify = new Button("修改");
        modify.getStyleClass().add("setting-control");
        modify.setOnAction(e -> showInfo("账户管理", "跳转至账户设置页面（演示）"));
        row.getChildren().addAll(email, modify);
        group.getChildren().add(row);
        return group;
    }

    private VBox buildShortcutContent() {
        VBox group = createSettingsGroup("快捷键");
        Label info = new Label("Ctrl + Shift + S 保存  |  Ctrl + F 搜索");
        info.getStyleClass().add("shortcut-info");
        Button customize = new Button("自定义");
        customize.getStyleClass().add("setting-control");
        customize.setOnAction(e -> showInfo("快捷键", "自定义快捷键功能（演示）"));
        HBox row = new HBox(16, info, customize);
        row.setAlignment(Pos.CENTER_LEFT);
        group.getChildren().add(row);
        return group;
    }

    private VBox buildCacheManagementContent() {
        VBox group = createSettingsGroup("缓存管理");
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Label size = new Label("已用缓存: 2.3 GB");
        size.getStyleClass().add("cache-size");
        Button clear = new Button("清理缓存");
        clear.getStyleClass().add("setting-control");
        clear.setOnAction(e -> showInfo("缓存管理", "缓存已清理（演示）"));
        row.getChildren().addAll(size, clear);
        group.getChildren().add(row);
        return group;
    }

    private VBox buildAISwitchContent() {
        VBox group = createSettingsGroup("AI辅助开关");
        CheckBox masterSwitch = new CheckBox("启用AI辅助功能");
        masterSwitch.setSelected(true);
        masterSwitch.getStyleClass().add("setting-checkbox");
        group.getChildren().add(masterSwitch);
        return group;
    }

    private VBox buildAutoClipContent() {
        VBox group = createSettingsGroup("自动剪辑");
        CheckBox autoClip = new CheckBox("自动检测并剪辑高光片段");
        autoClip.setSelected(true);
        autoClip.getStyleClass().add("setting-checkbox");
        group.getChildren().add(autoClip);
        return group;
    }

    private VBox buildSmartSubtitleContent() {
        VBox group = createSettingsGroup("智能字幕");
        CheckBox smartSub = new CheckBox("自动生成字幕");
        smartSub.setSelected(true);
        smartSub.getStyleClass().add("setting-checkbox");
        group.getChildren().add(smartSub);
        return group;
    }

    private VBox buildAIDetectionWeightContent() {
        VBox group = createSettingsGroup("AI检测权重");
        Label weightLabel = new Label("检测灵敏度: 0.75");
        weightLabel.getStyleClass().add("weight-label");
        Slider slider = new Slider(0, 1, 0.75);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.25);
        slider.setBlockIncrement(0.1);
        slider.getStyleClass().add("setting-slider");
        slider.valueProperty().addListener((obs, oldV, newV) ->
                weightLabel.setText(String.format("检测灵敏度: %.2f", newV.doubleValue())));
        VBox box = new VBox(8, weightLabel, slider);
        group.getChildren().add(box);
        return group;
    }

    private VBox buildVoiceAudioContent() {
        VBox group = createSettingsGroup("语音与音频");
        CheckBox enhance = new CheckBox("增强语音清晰度");
        enhance.setSelected(true);
        enhance.getStyleClass().add("setting-checkbox");
        group.getChildren().add(enhance);
        return group;
    }

    private VBox buildSavePathContent() {
        VBox group = createSettingsGroup("保存路径");
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        TextField pathField = new TextField(getDefaultSavePath());
        pathField.getStyleClass().add("setting-control");
        pathField.setPrefWidth(300);
        Button browse = new Button("浏览");
        browse.getStyleClass().add("setting-control");
        browse.setOnAction(e -> showInfo("保存路径", "选择文件夹（演示）"));
        row.getChildren().addAll(pathField, browse);
        group.getChildren().add(row);
        return group;
    }

    private VBox buildAutoSaveContent() {
        VBox group = createSettingsGroup("自动保存");
        CheckBox autoSave = new CheckBox("启用自动保存");
        autoSave.setSelected(true);
        autoSave.getStyleClass().add("setting-checkbox");
        Label intervalLabel = new Label("保存间隔: 5 分钟");
        intervalLabel.getStyleClass().add("auto-save-interval");
        Slider intervalSlider = new Slider(1, 30, 5);
        intervalSlider.setShowTickLabels(true);
        intervalSlider.setShowTickMarks(true);
        intervalSlider.setMajorTickUnit(5);
        intervalSlider.setBlockIncrement(1);
        intervalSlider.valueProperty().addListener((obs, oldV, newV) ->
                intervalLabel.setText(String.format("保存间隔: %.0f 分钟", newV.doubleValue())));
        VBox box = new VBox(8, autoSave, intervalLabel, intervalSlider);
        group.getChildren().add(box);
        return group;
    }

    private VBox buildDefaultExportContent() {
        VBox group = createSettingsGroup("默认导出设置");
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("MP4 (H.264)", "MOV (ProRes)", "AVI");
        formatCombo.setValue("MP4 (H.264)");
        formatCombo.getStyleClass().add("setting-control");
        ComboBox<String> resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll("1920x1080", "1280x720", "3840x2160");
        resolutionCombo.setValue("1920x1080");
        resolutionCombo.getStyleClass().add("setting-control");
        VBox options = new VBox(12);
        options.getChildren().addAll(
                new Label("格式:"), formatCombo,
                new Label("分辨率:"), resolutionCombo
        );
        group.getChildren().add(options);
        return group;
    }

    private VBox createSettingsGroup(String title) {
        VBox group = new VBox();
        group.getStyleClass().add("settings-group");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-group-title");
        group.getChildren().add(titleLabel);
        return group;
    }

    private HBox buildBottomStatsBar() {
        HBox bar = new HBox(32);
        bar.getStyleClass().add("bottom-stats-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        HBox memItem = new HBox(8);
        memItem.setAlignment(Pos.CENTER_LEFT);
        Label memLabel = new Label("APP已用内存:");
        memLabel.getStyleClass().add("stat-item");
        Label memValue = new Label("1.2 GB");
        memValue.getStyleClass().add("stat-value");
        memItem.getChildren().addAll(memLabel, memValue);

        HBox usedSpaceItem = new HBox(8);
        usedSpaceItem.setAlignment(Pos.CENTER_LEFT);
        Label usedLabel = new Label("设备已用空间:");
        usedLabel.getStyleClass().add("stat-item");
        Label usedValue = new Label("126 GB");
        usedValue.getStyleClass().add("stat-value");
        usedSpaceItem.getChildren().addAll(usedLabel, usedValue);

        HBox freeSpaceItem = new HBox(8);
        freeSpaceItem.setAlignment(Pos.CENTER_LEFT);
        Label freeLabel = new Label("设备可用空间:");
        freeLabel.getStyleClass().add("stat-item");
        Label freeValue = new Label("374 GB");
        freeValue.getStyleClass().add("stat-value");
        freeSpaceItem.getChildren().addAll(freeLabel, freeValue);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(memItem, usedSpaceItem, freeSpaceItem, spacer);
        return bar;
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    // ======================= 设置界面结束 =======================

    // 以下为原有方法，未作修改，仅保留
    private VBox buildRail() {
        VBox rail = new VBox(0);
        rail.setAlignment(Pos.TOP_LEFT);
        rail.setPrefWidth(78);
        rail.setMinWidth(78);
        rail.setMaxWidth(78);
        rail.setPadding(new Insets(0, 0, 0, 0));
        VBox railPill = new VBox();
        railPill.getStyleClass().add("rail-pill");
        railPill.setAlignment(Pos.TOP_CENTER);
        railPill.setMinHeight(0);
        railPill.setMaxHeight(Double.MAX_VALUE);
        railPill.setPrefWidth(78);
        railPill.setMinWidth(78);
        railPill.setMaxWidth(78);

        Label avatar = new Label();
        avatar.getStyleClass().add("avatar-dot");
        avatar.setMinSize(42, 42);
        avatar.setPrefSize(42, 42);
        avatar.setMaxSize(42, 42);

        Region pillSpacer = new Region();
        VBox.setVgrow(pillSpacer, Priority.ALWAYS);

        Label menu = new Label("\u2630");
        menu.getStyleClass().add("rail-menu-icon");

        railPill.getChildren().addAll(avatar, pillSpacer, menu);
        HBox pillRow = new HBox();
        pillRow.setAlignment(Pos.TOP_LEFT);
        pillRow.setPadding(new Insets(0, 0, 0, 0));
        VBox.setVgrow(pillRow, Priority.ALWAYS);
        pillRow.getChildren().add(railPill);
        railPill.prefHeightProperty().bind(pillRow.heightProperty());
        rail.getChildren().add(pillRow);
        return rail;
    }

    private HBox buildBrandBar() {
        HBox brandBar = new HBox(8);
        brandBar.getStyleClass().add("brand-bar");
        brandBar.setAlignment(Pos.CENTER_LEFT);
        Label logoMark = new Label("A");
        logoMark.getStyleClass().add("logo-mark");
        Label logoText = new Label("Alimoment");
        logoText.getStyleClass().add("logo-text");
        Label menuTag = new Label("菜单");
        menuTag.getStyleClass().add("brand-tag");
        HBox.setMargin(menuTag, new Insets(0, 0, 0, 12));
        brandBar.getChildren().addAll(logoMark, logoText, menuTag);
        return brandBar;
    }

    private HBox buildTopBar() {
        HBox top = new HBox(24);
        top.getStyleClass().add("top-bar");
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(9, 16, 9, 16));

        HBox nav = new HBox(16);

        VBox materialBtn = createNavItem("□", "素材库", false);
        VBox draftBtn = createNavItem("⌫", "草稿箱", false);
        VBox searchBtn = createNavItem("⌕", "智能检索", true);
        VBox clipBtn = createNavItem("✂", "智能剪辑", false);
        VBox settingsBtn = createNavItem("⛭", "设置", false);

        Collections.addAll(navButtons, materialBtn, draftBtn, searchBtn, clipBtn, settingsBtn);
        nav.getChildren().addAll(materialBtn, draftBtn, searchBtn, clipBtn, settingsBtn);

        for (VBox btn : navButtons) {
            btn.setOnMouseClicked(e -> {
                String name = ((Label) btn.getChildren().get(1)).getText();
                updateActiveButton(btn);
                if ("智能检索".equals(name)) {
                    switchToSmartSearch();
                } else if ("设置".equals(name)) {
                    switchToSettings();
                } else {
                    switchToBlank(name);
                }
            });
        }

        top.getChildren().addAll(nav);
        return top;
    }

    private void updateActiveButton(VBox activeButton) {
        for (VBox btn : navButtons) {
            Label icon = (Label) btn.getChildren().get(0);
            Label label = (Label) btn.getChildren().get(1);
            if (btn == activeButton) {
                btn.getStyleClass().add("menu-item-active");
                icon.getStyleClass().add("nav-active");
                label.getStyleClass().add("nav-active");
            } else {
                btn.getStyleClass().remove("menu-item-active");
                icon.getStyleClass().remove("nav-active");
                label.getStyleClass().remove("nav-active");
            }
        }
    }

    private VBox createNavItem(String iconText, String name, boolean active) {
        VBox item = new VBox(6);
        item.getStyleClass().add("nav-item");
        item.setAlignment(Pos.CENTER);

        Label icon = new Label(iconText);
        icon.getStyleClass().add("nav-icon");
        Label label = new Label(name);
        label.getStyleClass().add("nav-text");

        if (active) {
            item.getStyleClass().add("menu-item-active");
            icon.getStyleClass().add("nav-active");
            label.getStyleClass().add("nav-active");
        }
        item.getChildren().addAll(icon, label);
        return item;
    }

    private VBox buildStudioPane() {
        VBox studio = new VBox(14);
        studio.getStyleClass().add("studio-pane");

        StackPane preview = new StackPane();
        preview.getStyleClass().add("preview-area");
        VBox.setVgrow(preview, Priority.ALWAYS);

        Label play = new Label("▶");
        play.getStyleClass().add("play-button");
        preview.getChildren().add(play);

        HBox playerCtrl = new HBox();
        playerCtrl.getStyleClass().add("player-bar");
        Label currentTime = new Label("00:00 · 15/00/04:32");
        currentTime.getStyleClass().add("player-time");
        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);
        Label skip = new Label("⏮");
        Label playMini = new Label("▶");
        Label next = new Label("⏭");
        skip.getStyleClass().add("ctrl-icon");
        playMini.getStyleClass().add("ctrl-icon");
        next.getStyleClass().add("ctrl-icon");
        playerCtrl.getChildren().addAll(currentTime, ctrlSpacer, skip, playMini, next);

        StackPane timelineBox = new StackPane();
        timelineBox.getStyleClass().add("timeline-box");
        timelineBox.setMinHeight(112);
        timelineBox.setPrefHeight(112);
        timelineBox.setMaxHeight(112);
        HBox segments = new HBox();
        segments.setMinHeight(96);
        segments.setPrefHeight(96);
        segments.setMaxHeight(96);
        Label seg1 = new Label();
        Label seg2 = new Label();
        Label seg3 = new Label();
        Label seg4 = new Label();
        seg1.getStyleClass().add("seg-green");
        seg2.getStyleClass().add("seg-brown");
        seg3.getStyleClass().add("seg-gray");
        seg4.getStyleClass().add("seg-dark");
        seg1.setPrefWidth(210);
        seg2.setPrefWidth(120);
        seg3.setPrefWidth(80);
        seg4.setPrefWidth(220);
        segments.getChildren().addAll(seg1, seg2, seg3, seg4);
        Label playHead = new Label();
        playHead.getStyleClass().add("play-head");
        playHead.setMinWidth(2);
        playHead.setPrefWidth(2);
        playHead.setMaxWidth(2);
        playHead.setMinHeight(96);
        playHead.setPrefHeight(96);
        playHead.setMaxHeight(96);
        StackPane.setAlignment(playHead, Pos.CENTER_LEFT);
        StackPane.setMargin(playHead, new Insets(0, 0, 0, 330));
        timelineBox.getChildren().addAll(segments, playHead);

        studio.getChildren().addAll(preview, playerCtrl, timelineBox);
        return studio;
    }

    private VBox buildSearchPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("search-panel");
        panel.setPrefWidth(372);
        panel.setMinWidth(356);

        Label sectionLabel = new Label("智能搜索");
        sectionLabel.getStyleClass().add("section-meta-label");

        Label searchIcon = new Label("⌕");
        searchIcon.getStyleClass().add("search-icon");
        searchField = new TextArea();
        searchField.getStyleClass().add("search-input");
        searchField.setPromptText("描述您要查找的视频片段，例如：找到两个人在野外骑行的场景");
        searchField.setWrapText(true);
        searchField.setPrefRowCount(2);
        searchField.textProperty().addListener((obs, oldText, newText) -> filterResults(newText));
        HBox searchLine = new HBox(8);
        searchLine.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchLine.getChildren().addAll(searchIcon, searchField);

        HBox searchTools = new HBox(8);
        searchTools.getStyleClass().add("search-tools");
        searchTools.setAlignment(Pos.CENTER_RIGHT);
        Label tool1 = new Label("◻");
        Label tool2 = new Label("⊕");
        Label tool3 = new Label("⌁");
        Label tool4 = new Label("↑");
        tool1.getStyleClass().add("tool-icon");
        tool2.getStyleClass().add("tool-icon");
        tool3.getStyleClass().add("tool-icon");
        tool4.getStyleClass().addAll("tool-icon", "tool-icon-send");
        searchTools.getChildren().addAll(tool1, tool2, tool3, tool4);

        VBox searchInputWrap = new VBox(8);
        searchInputWrap.getStyleClass().add("search-input-wrap");
        searchInputWrap.setAlignment(Pos.TOP_LEFT);
        searchInputWrap.getChildren().addAll(searchLine, searchTools);

        VBox historyPane = buildHistoryPane();
        VBox resultPane = buildResultPane();
        VBox.setVgrow(resultPane, Priority.ALWAYS);

        panel.getChildren().addAll(sectionLabel, searchInputWrap, historyPane, resultPane);
        return panel;
    }

    private VBox buildHistoryPane() {
        VBox pane = new VBox(10);
        pane.setPrefWidth(280);
        pane.setMinWidth(280);
        pane.setMaxWidth(280);

        Label title = new Label("搜索历史");
        title.getStyleClass().add("panel-title");

        pane.getChildren().add(title);
        pane.getChildren().addAll(
                createHistoryItem("家长陪伴小朋友玩乐"),
                createHistoryItem("小朋友们一起搭积木"),
                createHistoryItem("小朋友们自己玩耍")
        );
        return pane;
    }

    private HBox createHistoryItem(String text) {
        HBox item = new HBox(8);
        item.getStyleClass().add("history-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("◷");
        icon.getStyleClass().add("history-icon");

        Label label = new Label(text);
        label.getStyleClass().add("history-text");

        item.getChildren().addAll(icon, label);
        item.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            searchField.setText(text);
            filterResults(text);
        });
        return item;
    }

    private VBox buildResultPane() {
        VBox pane = new VBox(10);
        Label title = new Label("搜索结果");
        title.getStyleClass().add("panel-title");

        resultListContainer.getStyleClass().add("result-list");
        emptyLabel.getStyleClass().add("empty-label");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        ScrollPane scrollPane = new ScrollPane(resultListContainer);
        scrollPane.getStyleClass().add("result-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        pane.getChildren().addAll(title, scrollPane, emptyLabel);
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }

    private void filterResults(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SearchResult> filtered;
        if (normalized.isEmpty()) {
            filtered = new ArrayList<>(allResults);
        } else {
            filtered = allResults.stream()
                    .filter(item -> item.description.toLowerCase(Locale.ROOT).contains(normalized))
                    .collect(Collectors.toList());
        }
        refreshResultCards(filtered);
    }

    private void refreshResultCards(List<SearchResult> items) {
        resultListContainer.getChildren().clear();

        if (items.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (SearchResult item : items) {
            resultListContainer.getChildren().add(createResultCard(item));
        }
    }

    private VBox createResultCard(SearchResult item) {
        VBox card = new VBox(6);
        card.getStyleClass().add("result-card");

        HBox topLine = new HBox(8);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label timeText = new Label(item.timeRange);
        timeText.getStyleClass().add("result-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label score = new Label((int) Math.round(item.score * 100) + "%");
        score.getStyleClass().add("match-score");
        if (item.score < 0.80) {
            score.getStyleClass().add("match-score-low");
        }
        topLine.getChildren().addAll(timeText, spacer, score);

        Label desc = new Label(item.description);
        desc.setWrapText(true);
        desc.getStyleClass().add("result-desc");

        card.getChildren().addAll(topLine, desc);

        card.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> showSelectedMessage(item.description));
        return card;
    }

    private void showSelectedMessage(String description) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText("已选中视频片段：" + description);
        alert.showAndWait();
    }

    private String getDefaultSavePath() {
        Path defaultPath = Paths.get(System.getProperty("user.home"), "Videos", "Alimoment");
        return defaultPath.toString();
    }

    private void applyCss(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class SearchResult {
        private final String timeRange;
        private final double score;
        private final String description;

        private SearchResult(String timeRange, double score, String description) {
            this.timeRange = timeRange;
            this.score = score;
            this.description = description;
        }
    }
}