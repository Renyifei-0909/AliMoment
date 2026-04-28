package com.aiimoment.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class DraftPageController {

    private static final String FILTER_BTN_STYLE = "draft-filter-btn";
    private static final String FILTER_BTN_ACTIVE_STYLE = "draft-filter-btn-active";
    private static final String VIEW_BTN_STYLE = "draft-view-btn";
    private static final String VIEW_BTN_ACTIVE_STYLE = "draft-view-btn-active";
    private static final String DRAFT_STORAGE_ROOT = "AIiMomentDrafts";

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
    private Button createFolderBtn;
    @FXML
    private ScrollPane contentScrollPane;
    @FXML
    private FlowPane gridPane;
    @FXML
    private VBox listBox;
    @FXML
    private VBox emptyStateBox;
    @FXML
    private TreeView<FolderNode> folderTreeView;
    @FXML
    private Label currentFolderLabel;

    private final Map<FilterType, Button> filterButtonMap = new EnumMap<>(FilterType.class);
    private List<DraftItem> drafts = List.of();
    private FilterType selectedFilter = FilterType.ALL;
    private ViewMode viewMode = ViewMode.GRID;
    private String selectedFolderPath = "";
    private TreeItem<FolderNode> folderRootItem;
    private TreeItem<FolderNode> allDraftsItem;

    @FXML
    public void initialize() {
        setupFolderTree();
        setupFilterButtons();
        setupViewButtons();
        setupSearch();
        setupScrollBehavior();
        setupActionButtons();
        loadMockDrafts();
        updateFilterStyles();
        updateViewStyles();
        refreshView();
    }

    private void setupFolderTree() {
        folderRootItem = new TreeItem<>(new FolderNode("ROOT", ""));
        folderRootItem.setExpanded(true);

        allDraftsItem = new TreeItem<>(new FolderNode("全部草稿", ""));
        allDraftsItem.setExpanded(true);
        folderRootItem.getChildren().add(allDraftsItem);

        TreeItem<FolderNode> projectA = addFolder(folderRootItem, "项目A");
        TreeItem<FolderNode> biking = addFolder(projectA, "骑行专题");
        addFolder(biking, "第一版");
        addFolder(biking, "终版");
        addFolder(projectA, "夜景素材");

        TreeItem<FolderNode> campaign = addFolder(folderRootItem, "宣传活动");
        TreeItem<FolderNode> spring = addFolder(campaign, "春季发布");
        addFolder(spring, "海报混剪");
        addFolder(campaign, "品牌短片");

        TreeItem<FolderNode> vlog = addFolder(folderRootItem, "Vlog");
        addFolder(vlog, "旅行");
        addFolder(vlog, "探店");

        folderTreeView.setRoot(folderRootItem);
        folderTreeView.setShowRoot(false);
        folderTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(FolderNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    return;
                }
                setText(item.name);
                ContextMenu menu = new ContextMenu();
                MenuItem reveal = new MenuItem("打开文件位置");
                reveal.setOnAction(e -> openFolderLocation(item));
                MenuItem rename = new MenuItem("重命名");
                rename.setOnAction(e -> renameFolder(getTreeItem()));
                MenuItem delete = new MenuItem("删除");
                delete.setOnAction(e -> deleteFolder(getTreeItem()));
                MenuItem create = new MenuItem("新建草稿夹");
                create.setOnAction(e -> showCreateFolderDialog(getTreeItem()));

                boolean locked = isLockedFolder(getTreeItem());
                rename.setDisable(locked);
                delete.setDisable(locked);

                menu.getItems().addAll(reveal, rename, delete, create);
                setContextMenu(menu);
            }
        });
        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null || newItem.getValue() == null) {
                selectedFolderPath = "";
                currentFolderLabel.setText("全部草稿");
            } else {
                selectedFolderPath = newItem.getValue().path;
                currentFolderLabel.setText(newItem.getValue().name);
            }
            refreshView();
        });
        folderTreeView.getSelectionModel().select(allDraftsItem);
    }

    private TreeItem<FolderNode> addFolder(TreeItem<FolderNode> parent, String name) {
        String parentPath = parent == null || parent.getValue() == null ? "" : parent.getValue().path;
        String path = parentPath == null || parentPath.isBlank() ? "/" + name : parentPath + "/" + name;
        TreeItem<FolderNode> item = new TreeItem<>(new FolderNode(name, path));
        item.setExpanded(true);
        if (parent != null) {
            parent.getChildren().add(item);
        }
        return item;
    }

    private void setupFilterButtons() {
        filterButtonMap.put(FilterType.ALL, allStatusBtn);
        filterButtonMap.put(FilterType.EDITING, editingStatusBtn);
        filterButtonMap.put(FilterType.COMPLETED, completedStatusBtn);
        filterButtonMap.values().forEach(btn -> {
            btn.setFocusTraversable(false);
            btn.setDefaultButton(false);
            btn.setCancelButton(false);
        });
        filterButtonMap.forEach((type, button) -> button.setOnAction(e -> {
            selectedFilter = type;
            updateFilterStyles();
            refreshView();
        }));
    }

    private void setupViewButtons() {
        gridViewBtn.setFocusTraversable(false);
        listViewBtn.setFocusTraversable(false);
        gridViewBtn.setDefaultButton(false);
        listViewBtn.setDefaultButton(false);
        gridViewBtn.setCancelButton(false);
        listViewBtn.setCancelButton(false);
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
        createFolderBtn.setOnAction(e -> showCreateFolderDialog());
    }

    private void showCreateFolderDialog() {
        TreeItem<FolderNode> selected = folderTreeView.getSelectionModel().getSelectedItem();
        showCreateFolderDialog(selected);
    }

    private void showCreateFolderDialog(TreeItem<FolderNode> fromItem) {
        TreeItem<FolderNode> parent = resolveCreateParent(fromItem);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新建草稿夹");
        dialog.setHeaderText(null);
        dialog.setContentText("草稿夹名称:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String name = result.get().trim();
        if (name.isBlank()) {
            return;
        }
        if (hasDuplicateFolder(parent, name)) {
            return;
        }
        TreeItem<FolderNode> newFolder = addFolder(parent, name);
        parent.setExpanded(true);
        folderTreeView.getSelectionModel().select(newFolder);
    }

    private TreeItem<FolderNode> resolveCreateParent(TreeItem<FolderNode> selected) {
        if (selected == null || selected == allDraftsItem) {
            return folderRootItem;
        }
        return selected;
    }

    private boolean hasDuplicateFolder(TreeItem<FolderNode> parent, String name) {
        if (parent == null) {
            return false;
        }
        for (TreeItem<FolderNode> child : parent.getChildren()) {
            if (child.getValue() != null && child.getValue().name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLockedFolder(TreeItem<FolderNode> item) {
        return item == null || item == folderRootItem || item == allDraftsItem;
    }

    private void loadMockDrafts() {
        List<DraftItem> source = new ArrayList<>();
        source.add(new DraftItem("1", "山地自行车精彩瞬间", "02:45", "2小时前", "2026-04-26", DraftStatus.EDITING, 65, "/项目A/骑行专题/第一版"));
        source.add(new DraftItem("2", "城市夜景延时摄影", "01:30", "5小时前", "2026-04-25", DraftStatus.EDITING, 40, "/项目A/夜景素材"));
        source.add(new DraftItem("3", "产品宣传片", "00:45", "昨天", "2026-04-24", DraftStatus.COMPLETED, null, "/宣传活动/品牌短片"));
        source.add(new DraftItem("4", "旅行Vlog第一集", "05:20", "2天前", "2026-04-22", DraftStatus.EDITING, 85, "/Vlog/旅行"));
        source.add(new DraftItem("5", "美食制作教程", "03:15", "3天前", "2026-04-21", DraftStatus.COMPLETED, null, "/Vlog/探店"));
        source.add(new DraftItem("6", "运动精彩集锦", "02:10", "1周前", "2026-04-18", DraftStatus.EDITING, 30, "/项目A/骑行专题/终版"));
        source.add(new DraftItem("7", "展会现场速剪", "01:05", "1周前", "2026-04-17", DraftStatus.EDITING, 72, "/宣传活动/春季发布"));
        source.add(new DraftItem("8", "门店宣传短片", "00:38", "8天前", "2026-04-16", DraftStatus.COMPLETED, null, "/宣传活动/春季发布/海报混剪"));
        source.add(new DraftItem("9", "体育赛事预告", "01:58", "9天前", "2026-04-15", DraftStatus.EDITING, 54, "/项目A/骑行专题"));
        source.add(new DraftItem("10", "校园活动混剪", "02:22", "10天前", "2026-04-14", DraftStatus.EDITING, 22, "/宣传活动/品牌短片"));
        source.add(new DraftItem("11", "采访精华版", "04:03", "11天前", "2026-04-13", DraftStatus.COMPLETED, null, "/项目A"));
        source.add(new DraftItem("12", "新品开箱", "01:16", "12天前", "2026-04-12", DraftStatus.EDITING, 91, "/Vlog/探店"));
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
                .filter(item -> matchesFolder(item.folderPath))
                .collect(Collectors.toList());
    }

    private boolean matchesFolder(String folderPath) {
        if (selectedFolderPath == null || selectedFolderPath.isBlank()) {
            return true;
        }
        if (folderPath == null || folderPath.isBlank()) {
            return false;
        }
        return folderPath.equals(selectedFolderPath) || folderPath.startsWith(selectedFolderPath + "/");
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
        installDraftContextMenu(card, item);
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
        installDraftContextMenu(row, item);
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

    private void installDraftContextMenu(Node target, DraftItem item) {
        if (target == null || item == null) {
            return;
        }
        ContextMenu menu = new ContextMenu();
        MenuItem reveal = new MenuItem("打开文件位置");
        reveal.setOnAction(e -> openDraftLocation(item));
        MenuItem rename = new MenuItem("重命名");
        rename.setOnAction(e -> renameDraft(item));
        MenuItem delete = new MenuItem("删除");
        delete.setOnAction(e -> deleteDraft(item));
        menu.getItems().addAll(reveal, rename, delete);
        target.setOnContextMenuRequested(e -> menu.show(target, e.getScreenX(), e.getScreenY()));
    }

    private void renameDraft(DraftItem item) {
        if (item == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog(item.title);
        dialog.setTitle("重命名草稿");
        dialog.setHeaderText(null);
        dialog.setContentText("草稿名称:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String newName = result.get().trim();
        if (newName.isBlank() || newName.equals(item.title)) {
            return;
        }
        item.title = newName;
        refreshView();
    }

    private void deleteDraft(DraftItem item) {
        if (item == null) {
            return;
        }
        drafts = drafts.stream()
                .filter(d -> !d.id.equals(item.id))
                .collect(Collectors.toCollection(ArrayList::new));
        refreshView();
    }

    private void renameFolder(TreeItem<FolderNode> item) {
        if (item == null || isLockedFolder(item) || item.getValue() == null) {
            return;
        }
        FolderNode node = item.getValue();
        TextInputDialog dialog = new TextInputDialog(node.name);
        dialog.setTitle("重命名草稿夹");
        dialog.setHeaderText(null);
        dialog.setContentText("草稿夹名称:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String newName = result.get().trim();
        if (newName.isBlank() || newName.equals(node.name)) {
            return;
        }
        TreeItem<FolderNode> parent = item.getParent();
        if (parent != null && hasDuplicateFolder(parent, newName)) {
            return;
        }
        String oldPath = node.path;
        String parentPath = parent != null && parent.getValue() != null ? parent.getValue().path : "";
        String newPath = parentPath == null || parentPath.isBlank() ? "/" + newName : parentPath + "/" + newName;
        node.name = newName;
        node.path = newPath;
        updateDescendantFolderPath(item, oldPath, newPath);
        updateDraftPathsByFolderRename(oldPath, newPath);
        if (oldPath.equals(selectedFolderPath)) {
            selectedFolderPath = newPath;
            currentFolderLabel.setText(newName);
        }
        folderTreeView.refresh();
        refreshView();
    }

    private void updateDescendantFolderPath(TreeItem<FolderNode> parentItem, String oldPrefix, String newPrefix) {
        for (TreeItem<FolderNode> child : parentItem.getChildren()) {
            if (child.getValue() != null && child.getValue().path != null) {
                child.getValue().path = child.getValue().path.replaceFirst(java.util.regex.Pattern.quote(oldPrefix), newPrefix);
            }
            updateDescendantFolderPath(child, oldPrefix, newPrefix);
        }
    }

    private void updateDraftPathsByFolderRename(String oldPrefix, String newPrefix) {
        for (DraftItem draft : drafts) {
            if (draft.folderPath == null || draft.folderPath.isBlank()) {
                continue;
            }
            if (draft.folderPath.equals(oldPrefix) || draft.folderPath.startsWith(oldPrefix + "/")) {
                draft.folderPath = draft.folderPath.replaceFirst(java.util.regex.Pattern.quote(oldPrefix), newPrefix);
            }
        }
    }

    private void deleteFolder(TreeItem<FolderNode> item) {
        if (item == null || isLockedFolder(item) || item.getValue() == null) {
            return;
        }
        String path = item.getValue().path;
        TreeItem<FolderNode> parent = item.getParent();
        if (parent != null) {
            parent.getChildren().remove(item);
        }
        drafts = drafts.stream()
                .filter(d -> !(d.folderPath.equals(path) || d.folderPath.startsWith(path + "/")))
                .collect(Collectors.toCollection(ArrayList::new));
        folderTreeView.getSelectionModel().select(allDraftsItem);
        refreshView();
    }

    private void openFolderLocation(FolderNode node) {
        Path base = resolveDraftStorageRoot();
        Path folder = node == null || node.path == null || node.path.isBlank()
                ? base
                : base.resolve(node.path.substring(1).replace("/", java.io.File.separator));
        revealInSystemExplorer(folder, false);
    }

    private void openDraftLocation(DraftItem item) {
        Path base = resolveDraftStorageRoot();
        Path folder = base;
        if (item.folderPath != null && !item.folderPath.isBlank()) {
            folder = base.resolve(item.folderPath.substring(1).replace("/", java.io.File.separator));
        }
        String fileName = safeFileName(item.title) + ".draft.json";
        Path draftFile = folder.resolve(fileName);
        revealInSystemExplorer(draftFile, true);
    }

    private Path resolveDraftStorageRoot() {
        return Path.of(System.getProperty("user.home"), "Documents", DRAFT_STORAGE_ROOT);
    }

    private void revealInSystemExplorer(Path target, boolean selectFile) {
        try {
            if (target == null) {
                return;
            }
            if (selectFile) {
                Files.createDirectories(target.getParent());
                if (!Files.exists(target)) {
                    Files.writeString(target, "{\n  \"note\": \"draft placeholder\"\n}\n");
                }
                new ProcessBuilder("explorer.exe", "/select," + target.toString()).start();
            } else {
                Files.createDirectories(target);
                new ProcessBuilder("explorer.exe", target.toString()).start();
            }
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("打开位置失败");
            alert.setHeaderText(null);
            alert.setContentText("无法打开文件资源管理器位置: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private String safeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "untitled-draft";
        }
        return raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
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
        private String title;
        private final String duration;
        private final String lastModified;
        private final String createdDate;
        private final DraftStatus status;
        private final Integer progress;
        private String folderPath;

        private DraftItem(String id,
                          String title,
                          String duration,
                          String lastModified,
                          String createdDate,
                          DraftStatus status,
                          Integer progress,
                          String folderPath) {
            this.id = id;
            this.title = title;
            this.duration = duration;
            this.lastModified = lastModified;
            this.createdDate = createdDate;
            this.status = status;
            this.progress = progress;
            this.folderPath = folderPath;
        }
    }

    private static class FolderNode {
        private String name;
        private String path;

        private FolderNode(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
