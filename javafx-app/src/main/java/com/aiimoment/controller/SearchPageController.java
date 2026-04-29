package com.aiimoment.controller;

import com.aiimoment.api.BackendApiClient;
import com.aiimoment.ui.AlimomentDialogs;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SearchPageController {

    @FXML
    private StackPane videoPane;
    @FXML
    private VBox videoPlaceholder;
    @FXML
    private Label videoPlaceholderHintLabel;
    @FXML
    private Label videoTimeLabel;
    @FXML
    private Button seekStartBtn;
    @FXML
    private Button playPauseBtn;
    @FXML
    private Button seekEndBtn;
    @FXML
    private TextArea searchField;
    @FXML
    private Button searchSubmitBtn;
    @FXML
    private ComboBox<BackendApiClient.AssetSummary> assetComboBox;
    @FXML
    private Label searchStatusLabel;
    @FXML
    private FlowPane historyPane;
    @FXML
    private VBox resultsBox;
    @FXML
    private Button railMenuBtn;
    @FXML
    private Pane timelinePane;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private VBox activeResultCard;
    private BackendApiClient.SearchResultItem activeResult;
    private final BackendApiClient apiClient = new BackendApiClient();
    private URI localVideoUri;
    private URI proxyVideoUri;
    private URI activeVideoUri;

    /** 当前播放进度占总时长比例 0..1，驱动索引条青色已播段宽度 */
    private final SimpleDoubleProperty playRatio = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty selectionStartRatio = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty selectionWidthRatio = new SimpleDoubleProperty(0);

    @FXML
    public void initialize() {
        configureAssetPicker();
        searchSubmitBtn.setOnAction(e -> onSearch());
        if (railMenuBtn != null) {
            railMenuBtn.setOnAction(e -> { /* 侧栏菜单占位 */ });
        }

        videoPane.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onVideoPaneClicked);
        seekStartBtn.setOnAction(e -> seekStart());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        seekEndBtn.setOnAction(e -> seekEnd());

        buildTimelineLayers();
        setSearchStatus("正在连接后端：" + apiClient.getBaseUrl());
        setSearchBusy(true);
        loadAssets();
    }

    private void buildTimelineLayers() {
        if (timelinePane == null) {
            return;
        }
        timelinePane.getChildren().clear();
        playRatio.set(0);

        Region gray = new Region();
        gray.getStyleClass().setAll("timeline-bg-layer");
        gray.setMouseTransparent(true);
        gray.prefWidthProperty().bind(timelinePane.widthProperty());
        gray.prefHeightProperty().bind(timelinePane.heightProperty());

        Region brown = new Region();
        brown.getStyleClass().setAll("timeline-index-band");
        brown.setMouseTransparent(true);
        brown.prefHeightProperty().bind(timelinePane.heightProperty());
        brown.prefWidthProperty().bind(timelinePane.widthProperty().multiply(0.15));
        brown.layoutXProperty().bind(timelinePane.widthProperty().multiply(0.22));

        Region teal = new Region();
        teal.getStyleClass().setAll("timeline-played-layer");
        teal.setMouseTransparent(true);
        teal.prefHeightProperty().bind(timelinePane.heightProperty());
        teal.prefWidthProperty().bind(Bindings.multiply(timelinePane.widthProperty(), playRatio));

        Region selection = new Region();
        selection.getStyleClass().setAll("timeline-selection-layer");
        selection.setMouseTransparent(true);
        selection.prefHeightProperty().bind(timelinePane.heightProperty());
        selection.prefWidthProperty().bind(Bindings.multiply(timelinePane.widthProperty(), selectionWidthRatio));
        selection.layoutXProperty().bind(Bindings.multiply(timelinePane.widthProperty(), selectionStartRatio));

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(timelinePane.widthProperty());
        clip.heightProperty().bind(timelinePane.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        timelinePane.setClip(clip);

        timelinePane.getChildren().addAll(gray, brown, selection, teal);
        timelinePane.setOnMouseClicked(this::onTimelineClicked);
    }

    private void onVideoPaneClicked(MouseEvent event) {
        if (mediaPlayer != null) {
            return;
        }
        pickAndLoadVideo();
    }

    private void onTimelineClicked(MouseEvent e) {
        if (mediaPlayer == null) {
            return;
        }
        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.isUnknown() || !total.greaterThan(Duration.ZERO)) {
            return;
        }
        double w = timelinePane.getWidth();
        if (w <= 1) {
            return;
        }
        double ratio = Math.min(1, Math.max(0, e.getX() / w));
        mediaPlayer.seek(Duration.seconds(total.toSeconds() * ratio));
    }

    private void pickAndLoadVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择视频文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("常见视频", "*.mp4", "*.m4v", "*.mkv", "*.avi", "*.mov", "*.wmv", "*.webm"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        File file = chooser.showOpenDialog(videoPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        localVideoUri = file.toURI();
        proxyVideoUri = null;
        loadVideo(localVideoUri, "本地视频", Duration.ZERO);
        setSearchStatus("本地视频已导入，正在向后端生成代理预览...");
        uploadPreviewProxy(file);
    }

    private void uploadPreviewProxy(File file) {
        CompletableFuture<BackendApiClient.UploadPayload> future = apiClient.uploadMedia(file);
        future.whenComplete((payload, error) -> Platform.runLater(() -> {
            if (error != null) {
                String message = BackendApiClient.describeError(error);
                setSearchStatus("代理预览生成失败，当前继续使用本地文件：" + message);
                return;
            }
            if (payload == null) {
                setSearchStatus("后端未返回代理预览信息，当前继续使用本地文件。");
                return;
            }
            if (payload.previewRelativeUrl != null && payload.previewRelativeUrl.startsWith("/media/previews/")) {
                String previewUrl = apiClient.resolveUrl(
                        payload.previewRelativeUrl != null && !payload.previewRelativeUrl.isBlank()
                                ? payload.previewRelativeUrl
                                : payload.previewUrl
                );
                proxyVideoUri = URI.create(previewUrl);
                Duration resumeAt = mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO;
                setSearchStatus("代理预览已生成，正在切换到稳定播放源...");
                loadVideo(proxyVideoUri, "代理视频", resumeAt);
                return;
            }
            if (payload.previewNote != null && !payload.previewNote.isBlank()) {
                setSearchStatus(payload.previewNote);
            } else {
                setSearchStatus("当前继续使用本地视频预览。");
            }
        }));
    }

    private void loadVideo(URI uri, String sourceLabel, Duration resumeAt) {
        disposeMedia();
        try {
            setVideoPlaceholderHint("正在加载视频预览...");
            Media media = new Media(uri.toString());
            media.setOnError(() -> Platform.runLater(() -> {
                Throwable err = media.getError();
                String msg = err != null ? err.getMessage() : "未知错误";
                boolean isProxy = proxyVideoUri != null && proxyVideoUri.equals(uri);
                boolean hasLocalFallback = localVideoUri != null && !localVideoUri.equals(uri);
                if (isProxy && hasLocalFallback) {
                    setSearchStatus("代理视频加载失败，已回退到本地视频播放。");
                    loadVideo(localVideoUri, "本地视频", Duration.ZERO);
                    return;
                }
                AlimomentDialogs.showError(videoPane.getScene().getWindow(), "无法加载视频", "无法加载该视频。\n\n" + msg);
                disposeMedia();
            }));

            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                Exception ex = mediaPlayer.getError();
                String msg = ex != null ? ex.getMessage() : "播放出错";
                boolean isProxy = proxyVideoUri != null && proxyVideoUri.equals(uri);
                boolean hasLocalFallback = localVideoUri != null && !localVideoUri.equals(uri);
                if (isProxy && hasLocalFallback) {
                    setSearchStatus("代理视频播放异常，已回退到本地视频。");
                    loadVideo(localVideoUri, "本地视频", Duration.ZERO);
                    return;
                }
                AlimomentDialogs.showError(videoPane.getScene().getWindow(), "播放失败", msg);
                disposeMedia();
            }));

            mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);
            mediaView.setMouseTransparent(true);
            mediaView.fitWidthProperty().bind(videoPane.widthProperty().subtract(8));
            mediaView.fitHeightProperty().bind(videoPane.heightProperty().subtract(8));
            videoPane.getChildren().add(0, mediaView);
            setSearchStatus("正在加载" + sourceLabel + "...");

            mediaPlayer.setOnReady(() -> {
                activeVideoUri = uri;
                videoPane.getStyleClass().remove("video-placeholder-empty");
                if (timelinePane != null) {
                    timelinePane.setCursor(Cursor.HAND);
                }
                if (media.getWidth() > 0 && media.getHeight() > 0) {
                    videoPlaceholder.setVisible(false);
                    videoPlaceholder.setManaged(false);
                    setSearchStatus(sourceLabel + "已载入，可点击检索结果直接跳转片段。");
                } else {
                    videoPlaceholder.setVisible(true);
                    videoPlaceholder.setManaged(true);
                    videoPlaceholder.toFront();
                    setVideoPlaceholderHint("视频已载入，但当前编码无法在 JavaFX 中渲染画面。\n建议改用 H.264 编码 MP4。");
                    setSearchStatus(sourceLabel + "已载入，但当前编码无法预览画面；仍可继续做检索联调。");
                }
                Duration total = mediaPlayer.getTotalDuration();
                if (resumeAt != null
                        && total != null
                        && !total.isUnknown()
                        && total.greaterThan(Duration.ZERO)
                        && resumeAt.greaterThan(Duration.ZERO)
                        && resumeAt.lessThan(total.subtract(Duration.seconds(0.2)))) {
                    mediaPlayer.seek(resumeAt);
                }
                updateTimeLabel();
                updateTimelineProgress();
                playPauseBtn.setText("⏸");
                mediaPlayer.play();
            });
            mediaPlayer.totalDurationProperty().addListener((o, oldDuration, newDuration) -> {
                updateTimeLabel();
                updateTimelineProgress();
            });
            mediaPlayer.currentTimeProperty().addListener((o, a, b) -> {
                updateTimeLabel();
                updateTimelineProgress();
            });
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.pause();
                playPauseBtn.setText("▶");
            });
        } catch (Exception ex) {
            AlimomentDialogs.showError(videoPane.getScene().getWindow(), "打开失败", "打开视频失败。\n\n" + ex.getMessage());
            disposeMedia();
        }
    }

    private void disposeMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (mediaView != null) {
            videoPane.getChildren().remove(mediaView);
            mediaView = null;
        }
        videoPlaceholder.setVisible(true);
        videoPlaceholder.setManaged(true);
        videoPlaceholder.toFront();
        setVideoPlaceholderHint("点击导入视频");
        if (!videoPane.getStyleClass().contains("video-placeholder-empty")) {
            videoPane.getStyleClass().add("video-placeholder-empty");
        }
        videoTimeLabel.setText("00:00:00 / 00:00:00");
        playPauseBtn.setText("▶");
        playRatio.set(0);
        if (activeResult != null) {
            updateSelectedSegment(activeResult);
        }
        if (timelinePane != null) {
            timelinePane.setCursor(Cursor.DEFAULT);
        }
        activeVideoUri = null;
    }

    private void setVideoPlaceholderHint(String text) {
        if (videoPlaceholderHintLabel != null) {
            videoPlaceholderHintLabel.setText(text);
        }
    }

    private void updateTimelineProgress() {
        if (mediaPlayer == null) {
            playRatio.set(0);
            return;
        }
        Duration cur = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
            playRatio.set(0);
            return;
        }
        double r = cur.toMillis() / total.toMillis();
        playRatio.set(Math.min(1, Math.max(0, r)));
    }

    private void updateTimeLabel() {
        if (mediaPlayer == null) {
            return;
        }
        Duration cur = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
            videoTimeLabel.setText(formatDuration(cur) + " / --:--:--");
            return;
        }
        videoTimeLabel.setText(formatDuration(cur) + " / " + formatDuration(total));
    }

    private static String formatDuration(Duration d) {
        if (d == null) {
            return "00:00:00";
        }
        int sec = (int) Math.floor(d.toSeconds());
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
    }

    private void seekStart() {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.seek(Duration.ZERO);
    }

    private void seekEnd() {
        if (mediaPlayer == null) {
            return;
        }
        Duration total = mediaPlayer.getTotalDuration();
        if (total != null && !total.isUnknown() && total.greaterThan(Duration.ZERO)) {
            double sec = Math.max(0, total.toSeconds() - 0.05);
            mediaPlayer.seek(Duration.seconds(sec));
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseBtn.setText("▶");
        } else {
            mediaPlayer.play();
            playPauseBtn.setText("⏸");
        }
    }

    private void onSearch() {
        String q = searchField.getText() != null ? searchField.getText().trim() : "";
        if (q.isEmpty()) {
            setSearchStatus("请输入检索描述后再发起检索。");
            return;
        }
        BackendApiClient.AssetSummary asset = assetComboBox != null ? assetComboBox.getValue() : null;
        if (asset == null || asset.assetId == null || asset.assetId.isBlank()) {
            setSearchStatus("素材库尚未准备好，请稍后重试。");
            return;
        }

        setSearchBusy(true);
        setSearchStatus("正在检索素材《" + asset.title + "》...");
        clearResults();

        apiClient.search(asset.assetId, q).whenComplete((payload, error) ->
                Platform.runLater(() -> {
                    setSearchBusy(false);
                    if (error != null) {
                        String message = BackendApiClient.describeError(error);
                        setSearchStatus("检索失败：" + message);
                        AlimomentDialogs.showError(videoPane.getScene().getWindow(), "检索失败", message);
                        return;
                    }
                    renderSearchResults(payload);
                }));
    }

    private void configureAssetPicker() {
        if (assetComboBox == null) {
            return;
        }

        assetComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BackendApiClient.AssetSummary asset) {
                if (asset == null) {
                    return "";
                }
                return asset.title + " · " + formatSeconds(asset.duration);
            }

            @Override
            public BackendApiClient.AssetSummary fromString(String string) {
                return null;
            }
        });
        assetComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(BackendApiClient.AssetSummary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title + " · " + formatSeconds(item.duration));
            }
        });
        assetComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            activeResult = null;
            activeResultCard = null;
            selectionStartRatio.set(0);
            selectionWidthRatio.set(0);
            clearResults();
            populateSuggestedQueries(newValue);
            if (newValue != null) {
                setSearchStatus("当前素材：" + newValue.title + "，可直接输入中文检索。");
            }
        });
    }

    private void loadAssets() {
        apiClient.fetchAssets().whenComplete((assets, error) ->
                Platform.runLater(() -> {
                    setSearchBusy(false);
                    if (error != null) {
                        String message = BackendApiClient.describeError(error);
                        setSearchStatus("素材库加载失败：" + message);
                        return;
                    }

                    List<BackendApiClient.AssetSummary> safeAssets = assets != null ? assets : Collections.emptyList();
                    assetComboBox.getItems().setAll(safeAssets);
                    if (safeAssets.isEmpty()) {
                        assetComboBox.setPromptText("暂无可用素材");
                        setSearchStatus("后端已连接，但当前没有可用演示素材。");
                        return;
                    }

                    assetComboBox.getSelectionModel().selectFirst();
                    setSearchStatus("素材库已连接，共 " + safeAssets.size() + " 个演示素材。");
                }));
    }

    private void renderSearchResults(BackendApiClient.SearchPayload payload) {
        clearResults();
        if (payload == null || payload.results == null || payload.results.isEmpty()) {
            setSearchStatus("未找到匹配片段，可以换一个描述再试。");
            resultsBox.getChildren().add(buildInfoCard("暂无结果", "这次检索没有返回可用片段。"));
            return;
        }

        VBox firstCard = null;
        BackendApiClient.SearchResultItem firstResult = null;
        for (BackendApiClient.SearchResultItem result : payload.results) {
            VBox card = buildResultCard(payload, result);
            if (firstCard == null) {
                firstCard = card;
                firstResult = result;
            }
            resultsBox.getChildren().add(card);
        }

        setSearchStatus("检索完成：原始词「" + payload.originalQuery + "」，检索词「" + payload.translatedQuery + "」，返回 " + payload.results.size() + " 个片段。");
        if (firstCard != null && firstResult != null) {
            activateResult(firstCard, firstResult);
        }
    }

    private VBox buildResultCard(BackendApiClient.SearchPayload payload, BackendApiClient.SearchResultItem result) {
        VBox card = new VBox(6);
        card.getStyleClass().add("result-card");
        card.setCursor(Cursor.HAND);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(formatSeconds(result.startTime) + " - " + formatSeconds(result.endTime));
        t.getStyleClass().add("result-time");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label badge = new Label(String.format("%d%%", Math.round(result.score * 100)));
        badge.getStyleClass().add(result.score >= 0.8 ? "badge-high" : "badge-mid");
        top.getChildren().addAll(t, sp, badge);

        String translated = payload.translatedQuery != null ? payload.translatedQuery : "";
        Label d = new Label("Top-" + result.rank + " 片段，相对匹配度展示，系统检索词：" + translated);
        d.getStyleClass().add("result-desc");
        d.setMaxWidth(Double.MAX_VALUE);
        d.setWrapText(true);

        VBox.setMargin(top, new Insets(0, 0, 0, 0));
        card.getChildren().addAll(top, d);
        card.setOnMouseClicked(event -> activateResult(card, result));
        return card;
    }

    private VBox buildInfoCard(String title, String desc) {
        VBox card = new VBox(6);
        card.getStyleClass().add("result-card");

        Label t = new Label(title);
        t.getStyleClass().add("result-time");

        Label d = new Label(desc);
        d.getStyleClass().add("result-desc");
        d.setWrapText(true);

        card.getChildren().addAll(t, d);
        return card;
    }

    private void activateResult(VBox card, BackendApiClient.SearchResultItem result) {
        if (activeResultCard != null) {
            activeResultCard.getStyleClass().remove("result-card-active");
        }
        activeResultCard = card;
        activeResult = result;
        if (!card.getStyleClass().contains("result-card-active")) {
            card.getStyleClass().add("result-card-active");
        }

        updateSelectedSegment(result);

        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(result.startTime));
            mediaPlayer.play();
            playPauseBtn.setText("⏸");
            setSearchStatus("已跳转到片段起点：" + formatSeconds(result.startTime));
        } else {
            setSearchStatus("已定位片段：" + formatSeconds(result.startTime) + " - " + formatSeconds(result.endTime) + "。导入同源视频后可直接预览。");
        }
    }

    private void updateSelectedSegment(BackendApiClient.SearchResultItem result) {
        BackendApiClient.AssetSummary asset = assetComboBox != null ? assetComboBox.getValue() : null;
        if (asset == null || asset.duration <= 0 || result == null) {
            selectionStartRatio.set(0);
            selectionWidthRatio.set(0);
            return;
        }
        double start = Math.max(0, Math.min(1, result.startTime / asset.duration));
        double end = Math.max(0, Math.min(1, result.endTime / asset.duration));
        selectionStartRatio.set(start);
        selectionWidthRatio.set(Math.max(0, end - start));
    }

    private void populateSuggestedQueries(BackendApiClient.AssetSummary asset) {
        if (historyPane == null) {
            return;
        }
        historyPane.getChildren().clear();
        List<String> queries = asset != null && asset.suggestedQueries != null ? asset.suggestedQueries : Collections.emptyList();
        if (queries.isEmpty()) {
            Button chip = new Button("暂无建议检索词");
            chip.getStyleClass().add("history-chip");
            chip.setDisable(true);
            historyPane.getChildren().add(chip);
            return;
        }

        for (String query : queries) {
            Button chip = new Button(query);
            chip.getStyleClass().add("history-chip");
            chip.setOnAction(ev -> searchField.setText(query));
            historyPane.getChildren().add(chip);
        }
    }

    private void clearResults() {
        resultsBox.getChildren().clear();
    }

    private void setSearchBusy(boolean busy) {
        searchSubmitBtn.setDisable(busy);
        if (assetComboBox != null) {
            assetComboBox.setDisable(busy);
        }
    }

    private void setSearchStatus(String message) {
        if (searchStatusLabel != null) {
            searchStatusLabel.setText(message);
        }
    }

    private static String formatSeconds(double seconds) {
        return formatDuration(Duration.seconds(seconds));
    }
}
