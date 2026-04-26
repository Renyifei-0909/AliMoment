package com.aiimoment.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DraftPageController {

    private static final String FILTER_BTN_STYLE = "draft-filter-btn";
    private static final String FILTER_BTN_ACTIVE_STYLE = "draft-filter-btn-active";
    private static final String VIEW_BTN_STYLE = "draft-view-btn";
    private static final String VIEW_BTN_ACTIVE_STYLE = "draft-view-btn-active";

    @FXML
    private TextField searchField;
    @FXML
    private Button allStatusBtn;
    @FXML
    private Button editingStatusBtn;
    @FXML
    private Button completedStatusBtn;
    @FXML
    private Button gridViewBtn;
    @FXML
    private Button listViewBtn;
    @FXML
    private Button newDraftBtn;
    @FXML
    private ScrollPane contentScrollPane;
    @FXML
    private FlowPane gridPane;
    @FXML
    private VBox listBox;
    @FXML
    private VBox emptyStateBox;

    private final Map<FilterType, Button> filterButtonMap = new EnumMap<>(FilterType.class);
    private List<DraftItem> drafts = List.of();
    private FilterType selectedFilter = FilterType.ALL;
    private ViewMode viewMode = ViewMode.GRID;

    @FXML
    public void initialize() {
        setupFilterButtons();
        setupViewButtons();
        setupSearch();
        setupScrollBehavior();
        setupActionButtons();
        loadMockDrafts();
        refreshView();
    }

    private void setupFilterButtons() {
        filterButtonMap.put(FilterType.ALL, allStatusBtn);
        filterButtonMap.put(FilterType.EDITING, editingStatusBtn);
        filterButtonMap.put(FilterType.COMPLETED, completedStatusBtn);
        filterButtonMap.forEach((type, button) -> button.setOnAction(e -> {
            selectedFilter = type;
            updateFilterStyles();
            refreshView();
        }));
    }

    private void setupViewButtons() {
        gridViewBtn.setOnAction(e -> {
            viewMode = ViewMode.GRID;
            updateViewStyles();
            refreshView();
        });
        listViewBtn.setOnAction(e -> {
            viewMode = ViewMode.LIST;
            updateViewStyles();
            refreshView();
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldText, newText) -> refreshView());
    }

    private void setupScrollBehavior() {
        contentScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            double width = Math.max(360, bounds.getWidth() - 28);
            gridPane.setPrefWrapLength(width);
        });
    }

    private void setupActionButtons() {
        newDraftBtn.setOnAction(e -> {
            // 演示入口，后续可接创建草稿流程
        });
    }

    private void loadMockDrafts() {
        List<DraftItem> source = new ArrayList<>();
        source.add(new DraftItem("1", "山地自行车精彩瞬间", "02:45", "2小时前", "2026-04-26", DraftStatus.EDITING, 65));
        source.add(new DraftItem("2", "城市夜景延时摄影", "01:30", "5小时前", "2026-04-25", DraftStatus.EDITING, 40));
        source.add(new DraftItem("3", "产品宣传片", "00:45", "昨天", "2026-04-24", DraftStatus.COMPLETED, null));
        source.add(new DraftItem("4", "旅行Vlog第一集", "05:20", "2天前", "2026-04-22", DraftStatus.EDITING, 85));
        source.add(new DraftItem("5", "美食制作教程", "03:15", "3天前", "2026-04-21", DraftStatus.COMPLETED, null));
        source.add(new DraftItem("6", "运动精彩集锦", "02:10", "1周前", "2026-04-18", DraftStatus.EDITING, 30));
        source.add(new DraftItem("7", "展会现场速剪", "01:05", "1周前", "2026-04-17", DraftStatus.EDITING, 72));
        source.add(new DraftItem("8", "门店宣传短片", "00:38", "8天前", "2026-04-16", DraftStatus.COMPLETED, null));
        source.add(new DraftItem("9", "体育赛事预告", "01:58", "9天前", "2026-04-15", DraftStatus.EDITING, 54));
        source.add(new DraftItem("10", "校园活动混剪", "02:22", "10天前", "2026-04-14", DraftStatus.EDITING, 22));
        source.add(new DraftItem("11", "采访精华版", "04:03", "11天前", "2026-04-13", DraftStatus.COMPLETED, null));
        source.add(new DraftItem("12", "新品开箱", "01:16", "12天前", "2026-04-12", DraftStatus.EDITING, 91));
        drafts = source;
    }

    private void refreshView() {
        List<DraftItem> filtered = filterDrafts();
        renderGrid(filtered);
        renderList(filtered);
        updateContentVisibility(filtered.isEmpty());
        contentScrollPane.setVvalue(0);
    }

    private List<DraftItem> filterDrafts() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        return drafts.stream()
                .filter(item -> selectedFilter == FilterType.ALL || mapFilter(item.status) == selectedFilter)
                .filter(item -> keyword.isBlank() || item.title.toLowerCase(Locale.ROOT).contains(keyword))
                .collect(Collectors.toList());
    }

    private void renderGrid(List<DraftItem> data) {
        gridPane.getChildren().clear();
        for (DraftItem item : data) {
            gridPane.getChildren().add(createGridCard(item));
        }
    }

    private VBox createGridCard(DraftItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("draft-grid-card");
        card.setMinWidth(230);
        card.setPrefWidth(230);
        card.setMaxWidth(230);

        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("draft-thumb");
        thumbnail.setMinHeight(132);
        Label icon = new Label("▶");
        icon.getStyleClass().add("draft-thumb-icon");
        thumbnail.getChildren().add(icon);

        Label duration = new Label(item.duration);
        duration.getStyleClass().add("draft-duration-badge");
        StackPane.setAlignment(duration, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(duration, new Insets(0, 8, 8, 0));
        thumbnail.getChildren().add(duration);

        HBox statusLine = new HBox();
        statusLine.setAlignment(Pos.TOP_LEFT);
        Label statusTag = createStatusTag(item.status);
        statusLine.getChildren().add(statusTag);
        StackPane.setAlignment(statusLine, Pos.TOP_LEFT);
        StackPane.setMargin(statusLine, new Insets(8, 0, 0, 8));
        thumbnail.getChildren().add(statusLine);

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 12, 12, 12));
        Label title = new Label(item.title);
        title.getStyleClass().add("draft-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setEllipsisString("...");

        body.getChildren().add(title);
        if (item.progress != null) {
            body.getChildren().add(buildProgressRow(item.progress));
        }

        HBox meta = new HBox();
        meta.setAlignment(Pos.CENTER_LEFT);
        Label left = new Label(item.lastModified);
        left.getStyleClass().add("draft-meta");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label right = new Label(item.createdDate);
        right.getStyleClass().add("draft-meta");
        meta.getChildren().addAll(left, spacer, right);

        body.getChildren().add(meta);
        card.getChildren().addAll(thumbnail, body);
        return card;
    }

    private void renderList(List<DraftItem> data) {
        listBox.getChildren().clear();
        for (DraftItem item : data) {
            listBox.getChildren().add(createListRow(item));
        }
    }

    private HBox createListRow(DraftItem item) {
        HBox row = new HBox(12);
        row.getStyleClass().add("draft-list-row");
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("draft-list-thumb");
        thumb.setMinSize(112, 64);
        thumb.setPrefSize(112, 64);
        Label icon = new Label("▶");
        icon.getStyleClass().add("draft-list-thumb-icon");
        thumb.getChildren().add(icon);

        Label duration = new Label(item.duration);
        duration.getStyleClass().add("draft-duration-badge-small");
        StackPane.setAlignment(duration, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(duration, new Insets(0, 6, 6, 0));
        thumb.getChildren().add(duration);

        VBox info = new VBox(7);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.title);
        title.getStyleClass().add("draft-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Label statusTag = createStatusTag(item.status);
        titleLine.getChildren().addAll(title, titleSpacer, statusTag);

        info.getChildren().add(titleLine);
        if (item.progress != null) {
            info.getChildren().add(buildProgressRow(item.progress));
        }

        Label meta = new Label("上次编辑: " + item.lastModified + "   创建于: " + item.createdDate);
        meta.getStyleClass().add("draft-meta");
        info.getChildren().add(meta);

        HBox actions = new HBox(8);
        actions.getChildren().addAll(
                createActionButton("编辑", "draft-action-btn-primary"),
                createActionButton("分享", "draft-action-btn-muted"),
                createActionButton("删除", "draft-action-btn-danger")
        );

        row.getChildren().addAll(thumb, info, actions);
        return row;
    }

    private HBox buildProgressRow(int progressValue) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label("编辑进度");
        text.getStyleClass().add("draft-progress-label");

        ProgressBar bar = new ProgressBar(progressValue / 100.0);
        bar.getStyleClass().add("draft-progress-bar");
        HBox.setHgrow(bar, Priority.ALWAYS);
        bar.setMaxWidth(Double.MAX_VALUE);

        Label value = new Label(progressValue + "%");
        value.getStyleClass().add("draft-progress-value");

        row.getChildren().addAll(text, bar, value);
        return row;
    }

    private Label createStatusTag(DraftStatus status) {
        Label tag = new Label(status == DraftStatus.EDITING ? "编辑中" : "已完成");
        tag.getStyleClass().add(status == DraftStatus.EDITING ? "draft-status-editing" : "draft-status-completed");
        return tag;
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setFocusTraversable(false);
        button.setOnAction(e -> {
            // 演示按钮动作
        });
        return button;
    }

    private void updateContentVisibility(boolean empty) {
        boolean grid = viewMode == ViewMode.GRID;
        setVisibleManaged(gridPane, grid && !empty);
        setVisibleManaged(listBox, !grid && !empty);
        setVisibleManaged(emptyStateBox, empty);
    }

    private void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateFilterStyles() {
        filterButtonMap.forEach((filterType, button) ->
                setActiveStyle(button, FILTER_BTN_STYLE, FILTER_BTN_ACTIVE_STYLE, selectedFilter == filterType));
    }

    private void updateViewStyles() {
        setActiveStyle(gridViewBtn, VIEW_BTN_STYLE, VIEW_BTN_ACTIVE_STYLE, viewMode == ViewMode.GRID);
        setActiveStyle(listViewBtn, VIEW_BTN_STYLE, VIEW_BTN_ACTIVE_STYLE, viewMode == ViewMode.LIST);
    }

    private void setActiveStyle(Button button, String baseStyle, String activeStyle, boolean active) {
        button.getStyleClass().removeAll(baseStyle, activeStyle);
        button.getStyleClass().add(baseStyle);
        if (active) {
            button.getStyleClass().add(activeStyle);
        }
    }

    private FilterType mapFilter(DraftStatus status) {
        return status == DraftStatus.EDITING ? FilterType.EDITING : FilterType.COMPLETED;
    }

    private enum ViewMode {
        GRID, LIST
    }

    private enum DraftStatus {
        EDITING, COMPLETED
    }

    private enum FilterType {
        ALL, EDITING, COMPLETED
    }

    private static class DraftItem {
        private final String id;
        private final String title;
        private final String duration;
        private final String lastModified;
        private final String createdDate;
        private final DraftStatus status;
        private final Integer progress;

        private DraftItem(String id,
                          String title,
                          String duration,
                          String lastModified,
                          String createdDate,
                          DraftStatus status,
                          Integer progress) {
            this.id = id;
            this.title = title;
            this.duration = duration;
            this.lastModified = lastModified;
            this.createdDate = createdDate;
            this.status = status;
            this.progress = progress;
        }
    }
}
