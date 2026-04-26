package com.aiimoment.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

public class MaterialPageController {

    private static final String FILTER_BTN_STYLE = "material-filter-btn";
    private static final String FILTER_BTN_ACTIVE_STYLE = "material-filter-btn-active";
    private static final String VIEW_BTN_STYLE = "material-view-btn";
    private static final String VIEW_BTN_ACTIVE_STYLE = "material-view-btn-active";

    @FXML
    private Button addMaterialBtn;
    @FXML
    private Button allBtn;
    @FXML
    private Button videoBtn;
    @FXML
    private Button imageBtn;
    @FXML
    private Button audioBtn;
    @FXML
    private Button textBtn;
    @FXML
    private Button gridViewBtn;
    @FXML
    private Button listViewBtn;
    @FXML
    private TextField searchField;
    @FXML
    private ScrollPane contentScrollPane;
    @FXML
    private FlowPane gridPane;
    @FXML
    private VBox listBox;
    @FXML
    private Label emptyStateLabel;

    private final Map<MaterialType, Button> typeButtonMap = new EnumMap<>(MaterialType.class);
    private List<MaterialItem> materials = List.of();
    private MaterialType selectedType = MaterialType.ALL;
    private ViewMode viewMode = ViewMode.GRID;

    @FXML
    public void initialize() {
        setupTypeButtons();
        setupViewButtons();
        setupSearch();
        setupActionButtons();
        setupScrollBehavior();
        loadMockMaterials();
        refreshView();
    }

    private void setupTypeButtons() {
        typeButtonMap.put(MaterialType.ALL, allBtn);
        typeButtonMap.put(MaterialType.VIDEO, videoBtn);
        typeButtonMap.put(MaterialType.IMAGE, imageBtn);
        typeButtonMap.put(MaterialType.AUDIO, audioBtn);
        typeButtonMap.put(MaterialType.TEXT, textBtn);

        typeButtonMap.forEach((type, button) -> button.setOnAction(e -> {
            selectedType = type;
            updateTypeButtonStyles();
            refreshView();
        }));
    }

    private void setupViewButtons() {
        gridViewBtn.setOnAction(e -> {
            viewMode = ViewMode.GRID;
            updateViewModeButtonStyles();
            refreshView();
        });
        listViewBtn.setOnAction(e -> {
            viewMode = ViewMode.LIST;
            updateViewModeButtonStyles();
            refreshView();
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldText, newText) -> refreshView());
    }

    private void setupActionButtons() {
        addMaterialBtn.setOnAction(e -> {
            // 演示入口：后续可在这里接真实上传流程
        });
    }

    private void setupScrollBehavior() {
        contentScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            double width = Math.max(320, bounds.getWidth() - 24);
            gridPane.setPrefWrapLength(width);
        });
    }

    private void loadMockMaterials() {
        List<MaterialItem> source = new ArrayList<>();
        source.add(new MaterialItem("1", "山地自行车.mp4", MaterialType.VIDEO, "00:45", "128 MB", "2026-04-20"));
        source.add(new MaterialItem("2", "城市风光.mp4", MaterialType.VIDEO, "01:20", "256 MB", "2026-04-19"));
        source.add(new MaterialItem("3", "日落.jpg", MaterialType.IMAGE, null, "2.5 MB", "2026-04-18"));
        source.add(new MaterialItem("4", "背景音乐.mp3", MaterialType.AUDIO, "03:25", "8 MB", "2026-04-17"));
        source.add(new MaterialItem("5", "产品展示.mp4", MaterialType.VIDEO, "00:30", "95 MB", "2026-04-16"));
        source.add(new MaterialItem("6", "风景照.jpg", MaterialType.IMAGE, null, "3.2 MB", "2026-04-15"));
        source.add(new MaterialItem("7", "片头音效.mp3", MaterialType.AUDIO, "00:05", "1.2 MB", "2026-04-14"));
        source.add(new MaterialItem("8", "字幕文本.txt", MaterialType.TEXT, null, "15 KB", "2026-04-13"));
        source.add(new MaterialItem("9", "节奏鼓点.mp3", MaterialType.AUDIO, "00:38", "2.8 MB", "2026-04-12"));
        source.add(new MaterialItem("10", "开场片段.mp4", MaterialType.VIDEO, "00:18", "72 MB", "2026-04-11"));
        source.add(new MaterialItem("11", "封面图.jpg", MaterialType.IMAGE, null, "1.8 MB", "2026-04-10"));
        source.add(new MaterialItem("12", "说明文案.txt", MaterialType.TEXT, null, "9 KB", "2026-04-09"));
        source.add(new MaterialItem("13", "人群镜头.mp4", MaterialType.VIDEO, "00:52", "146 MB", "2026-04-08"));
        source.add(new MaterialItem("14", "无人机航拍.mp4", MaterialType.VIDEO, "01:45", "412 MB", "2026-04-07"));
        source.add(new MaterialItem("15", "旁白草稿.txt", MaterialType.TEXT, null, "31 KB", "2026-04-06"));
        source.add(new MaterialItem("16", "海浪声.mp3", MaterialType.AUDIO, "01:08", "6.1 MB", "2026-04-05"));
        materials = source;
    }

    private void refreshView() {
        List<MaterialItem> filtered = filterMaterials();
        renderGrid(filtered);
        renderList(filtered);
        updateContentVisibility(filtered.isEmpty());
        contentScrollPane.setVvalue(0);
    }

    private List<MaterialItem> filterMaterials() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        return materials.stream()
                .filter(item -> selectedType == MaterialType.ALL || item.type == selectedType)
                .filter(item -> keyword.isEmpty() || item.name.toLowerCase(Locale.ROOT).contains(keyword))
                .collect(Collectors.toList());
    }

    private void renderGrid(List<MaterialItem> data) {
        gridPane.getChildren().clear();
        for (MaterialItem item : data) {
            gridPane.getChildren().add(createGridCard(item));
        }
    }

    private VBox createGridCard(MaterialItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("material-grid-card");
        card.setMinWidth(188);
        card.setPrefWidth(188);
        card.setMaxWidth(188);

        StackPane preview = new StackPane();
        preview.getStyleClass().add("material-preview");
        Label typeIcon = new Label(iconFor(item.type));
        typeIcon.getStyleClass().add("material-preview-icon");
        preview.getChildren().add(typeIcon);
        if (item.duration != null && !item.duration.isBlank()) {
            Label duration = new Label(item.duration);
            duration.getStyleClass().add("material-duration-badge");
            StackPane.setMargin(duration, new Insets(0, 8, 8, 0));
            StackPane.setAlignment(duration, javafx.geometry.Pos.BOTTOM_RIGHT);
            preview.getChildren().add(duration);
        }

        VBox textMeta = new VBox(4);
        textMeta.setPadding(new Insets(10, 12, 12, 12));
        Label name = new Label(item.name);
        name.getStyleClass().add("material-name");
        Label desc = new Label(item.size + "   " + item.date);
        desc.getStyleClass().add("material-meta");
        textMeta.getChildren().addAll(name, desc);

        card.getChildren().addAll(preview, textMeta);
        return card;
    }

    private void renderList(List<MaterialItem> data) {
        listBox.getChildren().clear();
        for (MaterialItem item : data) {
            listBox.getChildren().add(createListRow(item));
        }
    }

    private HBox createListRow(MaterialItem item) {
        HBox row = new HBox(12);
        row.getStyleClass().add("material-list-row");
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setMinHeight(72);

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("material-list-thumb");
        thumb.setMinSize(92, 52);
        thumb.setPrefSize(92, 52);
        Label icon = new Label(iconFor(item.type));
        icon.getStyleClass().add("material-list-thumb-icon");
        thumb.getChildren().add(icon);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(item.name);
        name.getStyleClass().add("material-name");

        String detail = item.duration == null || item.duration.isBlank()
                ? item.size + "   " + item.date
                : item.duration + "   " + item.size + "   " + item.date;
        Label meta = new Label(detail);
        meta.getStyleClass().add("material-meta");
        info.getChildren().addAll(name, meta);

        Label tag = new Label(typeLabel(item.type));
        tag.getStyleClass().add("material-type-tag");

        row.getChildren().addAll(thumb, info, tag);
        return row;
    }

    private void updateContentVisibility(boolean empty) {
        boolean grid = viewMode == ViewMode.GRID;
        setVisibleManaged(gridPane, grid && !empty);
        setVisibleManaged(listBox, !grid && !empty);
        setVisibleManaged(emptyStateLabel, empty);
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateTypeButtonStyles() {
        typeButtonMap.forEach((type, btn) -> setActiveStyle(btn, FILTER_BTN_STYLE, FILTER_BTN_ACTIVE_STYLE, type == selectedType));
    }

    private void updateViewModeButtonStyles() {
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

    private String iconFor(MaterialType type) {
        switch (type) {
            case VIDEO:
                return "▶";
            case IMAGE:
                return "🖼";
            case AUDIO:
                return "♪";
            case TEXT:
                return "T";
            case ALL:
            default:
                return "•";
        }
    }

    private String typeLabel(MaterialType type) {
        switch (type) {
            case VIDEO:
                return "视频";
            case IMAGE:
                return "图片";
            case AUDIO:
                return "音频";
            case TEXT:
                return "文本";
            case ALL:
            default:
                return "全部";
        }
    }

    private enum MaterialType {
        ALL, VIDEO, IMAGE, AUDIO, TEXT
    }

    private enum ViewMode {
        GRID, LIST
    }

    private static class MaterialItem {
        private final String id;
        private final String name;
        private final MaterialType type;
        private final String duration;
        private final String size;
        private final String date;

        private MaterialItem(String id, String name, MaterialType type, String duration, String size, String date) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.duration = duration;
            this.size = size;
            this.date = date;
        }
    }

}
