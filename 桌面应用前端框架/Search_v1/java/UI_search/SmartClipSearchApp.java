package UI_research;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(0, 0, 0, 0));

        HBox brandAtTopLeft = buildBrandBar();
        brandAtTopLeft.setAlignment(Pos.TOP_LEFT);
        brandAtTopLeft.setPadding(new Insets(0, 0, 40, 0));
        root.setTop(brandAtTopLeft);

        root.setLeft(buildRail());
        root.setCenter(buildMainArea());

        Scene scene = new Scene(root, 1600, 920);
        applyCss(scene);

        stage.setTitle("Alimoment - 智能检索");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();

        filterResults("");
    }

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

    private VBox buildMainArea() {
        VBox main = new VBox(10);
        main.getStyleClass().add("main-content");
        main.setPadding(new Insets(0, 0, 12, 0));
        main.setAlignment(Pos.TOP_CENTER);

        HBox topBar = buildTopBar();
        topBar.setMaxWidth(Double.MAX_VALUE);

        HBox workspace = new HBox(16);
        workspace.setMaxWidth(Double.MAX_VALUE);
        VBox leftStudio = buildStudioPane();
        VBox rightSearchPanel = buildSearchPanel();
        HBox.setHgrow(leftStudio, Priority.ALWAYS);
        workspace.getChildren().addAll(leftStudio, rightSearchPanel);

        StackPane topBarWrap = new StackPane(topBar);
        topBarWrap.setAlignment(Pos.TOP_CENTER);
        topBarWrap.setPadding(new Insets(0, 14, 0, 14));

        StackPane workspaceWrap = new StackPane(workspace);
        workspaceWrap.setAlignment(Pos.TOP_CENTER);
        workspaceWrap.setPadding(new Insets(0, 16, 0, 16));

        VBox.setVgrow(workspace, Priority.ALWAYS);
        VBox.setVgrow(workspaceWrap, Priority.ALWAYS);
        main.getChildren().addAll(topBarWrap, workspaceWrap);
        return main;
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
        nav.getChildren().addAll(
                createNavItem("□", "素材库", false),
                createNavItem("⌫", "草稿箱", false),
                createNavItem("⌕", "智能检索", true),
                createNavItem("✂", "智能剪辑", false),
                createNavItem("⛭", "设置", false)
        );

        top.getChildren().addAll(nav);
        return top;
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
