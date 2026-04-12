package com.aiimoment.controller;

import com.aiimoment.ui.AlimomentDialogs;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

import java.io.File;
import java.net.URI;
import java.util.Locale;

public class SearchPageController {

    @FXML
    private StackPane videoPane;
    @FXML
    private VBox videoPlaceholder;
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
    private FlowPane historyPane;
    @FXML
    private VBox resultsBox;
    @FXML
    private Button railMenuBtn;
    @FXML
    private Pane timelinePane;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;

    /** 当前播放进度占总时长比例 0..1，驱动索引条青色已播段宽度 */
    private final SimpleDoubleProperty playRatio = new SimpleDoubleProperty(0);

    @FXML
    public void initialize() {
        loadSampleResults();
        searchSubmitBtn.setOnAction(e -> onSearch());
        if (historyPane != null) {
            for (var n : historyPane.getChildren()) {
                if (n instanceof Button) {
                    Button chip = (Button) n;
                    chip.setOnAction(ev -> searchField.setText(chip.getText()));
                }
            }
        }
        if (railMenuBtn != null) {
            railMenuBtn.setOnAction(e -> { /* 侧栏菜单占位 */ });
        }

        videoPane.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onVideoPaneClicked);
        seekStartBtn.setOnAction(e -> seekStart());
        playPauseBtn.setOnAction(e -> togglePlayPause());
        seekEndBtn.setOnAction(e -> seekEnd());

        buildTimelineLayers();
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

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(timelinePane.widthProperty());
        clip.heightProperty().bind(timelinePane.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        timelinePane.setClip(clip);

        timelinePane.getChildren().addAll(gray, brown, teal);
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
        loadVideo(file.toURI());
    }

    private void loadVideo(URI uri) {
        disposeMedia();
        try {
            Media media = new Media(uri.toString());
            media.setOnError(() -> Platform.runLater(() -> {
                Throwable err = media.getError();
                String msg = err != null ? err.getMessage() : "未知错误";
                AlimomentDialogs.showError(videoPane.getScene().getWindow(), "无法加载视频", "无法加载该视频。\n\n" + msg);
                disposeMedia();
            }));

            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                Exception ex = mediaPlayer.getError();
                String msg = ex != null ? ex.getMessage() : "播放出错";
                AlimomentDialogs.showError(videoPane.getScene().getWindow(), "播放失败", msg);
                disposeMedia();
            }));

            mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.fitWidthProperty().bind(videoPane.widthProperty().subtract(8));
            mediaView.fitHeightProperty().bind(videoPane.heightProperty().subtract(8));
            videoPane.getChildren().add(0, mediaView);

            videoPlaceholder.setVisible(false);
            videoPlaceholder.setManaged(false);
            videoPane.getStyleClass().remove("video-placeholder-empty");
            if (timelinePane != null) {
                timelinePane.setCursor(Cursor.HAND);
            }

            mediaPlayer.setOnReady(() -> {
                updateTimeLabel();
                updateTimelineProgress();
                playPauseBtn.setText("⏸");
            });
            mediaPlayer.currentTimeProperty().addListener((o, a, b) -> {
                updateTimeLabel();
                updateTimelineProgress();
            });
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.pause();
                playPauseBtn.setText("▶");
            });

            mediaPlayer.play();
            playPauseBtn.setText("⏸");
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
        if (!videoPane.getStyleClass().contains("video-placeholder-empty")) {
            videoPane.getStyleClass().add("video-placeholder-empty");
        }
        videoTimeLabel.setText("00:00:00 / 00:00:00");
        playPauseBtn.setText("▶");
        playRatio.set(0);
        if (timelinePane != null) {
            timelinePane.setCursor(Cursor.DEFAULT);
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
            return;
        }
        resultsBox.getChildren().add(0, buildResultCard("00:00:00 - 00:00:12", "与检索相关：" + q, 0.78));
    }

    private void loadSampleResults() {
        resultsBox.getChildren().add(buildResultCard(
                "00:01:23 - 00:01:35",
                "小女孩在公园中央的草地上快乐的奔跑，阳光洒在她脸上。",
                0.92));
        resultsBox.getChildren().add(buildResultCard(
                "00:02:45 - 00:03:10",
                "小女孩穿着粉色裙子在喷泉旁旋转跳舞",
                0.87));
        resultsBox.getChildren().add(buildResultCard(
                "00:03:50 - 00:04:15",
                "小女孩和小狗在沙坑里一起玩耍的画面",
                0.76));
    }

    private static VBox buildResultCard(String time, String desc, double score) {
        VBox card = new VBox(6);
        card.getStyleClass().add("result-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(time);
        t.getStyleClass().add("result-time");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label badge = new Label(String.format("%d%%", Math.round(score * 100)));
        badge.getStyleClass().add(score >= 0.85 ? "badge-high" : "badge-mid");
        top.getChildren().addAll(t, sp, badge);

        Label d = new Label(desc);
        d.getStyleClass().add("result-desc");
        d.setMaxWidth(Double.MAX_VALUE);
        d.setWrapText(true);

        VBox.setMargin(top, new Insets(0, 0, 0, 0));
        card.getChildren().addAll(top, d);
        return card;
    }
}
