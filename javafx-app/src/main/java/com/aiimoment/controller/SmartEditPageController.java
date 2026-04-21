package com.aiimoment.controller;

import com.aiimoment.api.BackendApiClient;
import com.aiimoment.ui.AlimomentDialogs;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.effect.ColorAdjust;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URI;
import java.util.Locale;

public class SmartEditPageController {
    @FXML
    private Button splitBtn;
    @FXML
    private Button speedBtn;
    @FXML
    private Button trimBtn;
    @FXML
    private Button effectBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button oneClickEditBtn;
    @FXML
    private Button undoBtn;
    @FXML
    private Button redoBtn;
    @FXML
    private Slider speedSlider;
    @FXML
    private Slider trimSlider;
    @FXML
    private Slider effectSlider;
    @FXML
    private TextArea demandInput;
    @FXML
    private CheckBox autoVoiceCheck;
    @FXML
    private Label toolStatusLabel;
    @FXML
    private Label resultStatusLabel;
    @FXML
    private StackPane previewPane;
    @FXML
    private VBox previewPlaceholder;
    @FXML
    private VBox resultListBox;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Duration clipStart = Duration.ZERO;
    private File selectedVideoFile;
    private String uploadedMediaId;
    private final BackendApiClient apiClient = new BackendApiClient();

    @FXML
    public void initialize() {
        previewPane.setOnMouseClicked(e -> {
            if (mediaPlayer == null) {
                pickAndLoadVideo();
            }
        });

        splitBtn.setOnAction(e -> onSplit());
        speedBtn.setOnAction(e -> onSpeed());
        trimBtn.setOnAction(e -> onTrim());
        effectBtn.setOnAction(e -> onEffect());
        deleteBtn.setOnAction(e -> onDeleteClip());

        undoBtn.setOnAction(e -> toolStatusLabel.setText("已撤销上一步操作"));
        redoBtn.setOnAction(e -> toolStatusLabel.setText("已恢复上一步操作"));

        oneClickEditBtn.setOnAction(e -> onOneClickEdit());
    }

    private void onSplit() {
        if (mediaPlayer == null) {
            toolStatusLabel.setText("请先在预览区导入视频，再执行分割。");
            return;
        }
        clipStart = mediaPlayer.getCurrentTime();
        toolStatusLabel.setText("分割点已记录：" + formatDuration(clipStart));
    }

    private void onSpeed() {
        if (mediaPlayer == null) {
            toolStatusLabel.setText("请先导入视频，再设置变速。");
            return;
        }
        double rate = speedSlider.getValue();
        mediaPlayer.setRate(rate);
        toolStatusLabel.setText("已应用变速：x" + String.format(Locale.ROOT, "%.2f", rate));
    }

    private void onTrim() {
        if (mediaPlayer == null) {
            toolStatusLabel.setText("请先导入视频，再执行剪辑。");
            return;
        }
        Duration current = mediaPlayer.getCurrentTime();
        double keep = trimSlider.getValue();
        double startSec = Math.max(0, current.toSeconds() - keep / 2.0);
        double endSec = startSec + keep;
        Duration total = mediaPlayer.getTotalDuration();
        if (total != null && !total.isUnknown()) {
            endSec = Math.min(endSec, total.toSeconds());
        }
        clipStart = Duration.seconds(startSec);
        mediaPlayer.seek(clipStart);
        toolStatusLabel.setText("剪辑范围已选：" + formatDuration(Duration.seconds(startSec)) + " - " + formatDuration(Duration.seconds(endSec)));
    }

    private void onEffect() {
        if (mediaView == null) {
            toolStatusLabel.setText("请先导入视频，再添加特效。");
            return;
        }
        double intensity = effectSlider.getValue() / 100.0;
        ColorAdjust adjust = new ColorAdjust();
        adjust.setSaturation(-0.25 * intensity);
        adjust.setContrast(0.15 * intensity);
        adjust.setBrightness(0.08 * intensity);
        mediaView.setEffect(adjust);
        toolStatusLabel.setText("已应用特效，强度：" + (int) effectSlider.getValue() + "%");
    }

    private void onDeleteClip() {
        disposeMedia();
        toolStatusLabel.setText("已删除当前片段，预览区已清空。");
    }

    private void pickAndLoadVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择视频文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("常见视频", "*.mp4", "*.m4v", "*.mkv", "*.avi", "*.mov", "*.wmv", "*.webm"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        File file = chooser.showOpenDialog(previewPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        selectedVideoFile = file;
        uploadedMediaId = null;
        loadVideo(file.toURI());
    }

    private void loadVideo(URI uri) {
        disposeMedia();
        try {
            Media media = new Media(uri.toString());
            media.setOnError(() -> Platform.runLater(() -> {
                Throwable err = media.getError();
                String msg = err != null ? err.getMessage() : "未知错误";
                AlimomentDialogs.showError(previewPane.getScene().getWindow(), "无法加载视频", "无法加载该视频。\n\n" + msg);
                disposeMedia();
            }));
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                Exception ex = mediaPlayer.getError();
                String msg = ex != null ? ex.getMessage() : "播放出错";
                AlimomentDialogs.showError(previewPane.getScene().getWindow(), "播放失败", msg);
                disposeMedia();
            }));
            mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);
            mediaView.setMouseTransparent(true);
            mediaView.fitWidthProperty().bind(previewPane.widthProperty().subtract(8));
            mediaView.fitHeightProperty().bind(previewPane.heightProperty().subtract(8));
            previewPane.getChildren().add(0, mediaView);
            toolStatusLabel.setText("正在加载视频预览...");
            mediaPlayer.setOnReady(() -> {
                previewPlaceholder.setVisible(false);
                previewPlaceholder.setManaged(false);
                previewPane.getStyleClass().remove("smart-preview-empty");
                previewPane.setCursor(Cursor.DEFAULT);
                mediaPlayer.play();
                toolStatusLabel.setText("视频已导入，可进行分割、变速、剪辑、特效操作。");
            });
        } catch (Exception ex) {
            AlimomentDialogs.showError(previewPane.getScene().getWindow(), "打开失败", "打开视频失败。\n\n" + ex.getMessage());
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
            previewPane.getChildren().remove(mediaView);
            mediaView = null;
        }
        previewPlaceholder.setVisible(true);
        previewPlaceholder.setManaged(true);
        if (!previewPane.getStyleClass().contains("smart-preview-empty")) {
            previewPane.getStyleClass().add("smart-preview-empty");
        }
        previewPane.setCursor(Cursor.HAND);
        clipStart = Duration.ZERO;
    }

    private void onOneClickEdit() {
        String demand = demandInput.getText() == null ? "" : demandInput.getText().trim();
        if (demand.isEmpty()) {
            resultStatusLabel.setText("请先输入创作需求，再执行一键剪辑。");
            return;
        }
        if (selectedVideoFile == null) {
            resultStatusLabel.setText("请先导入本地视频，再执行一键剪辑。");
            return;
        }

        Duration[] segment = resolveEditSegment();
        double start = segment[0].toSeconds();
        double end = segment[1].toSeconds();
        if (end <= start) {
            resultStatusLabel.setText("当前剪辑范围无效，请调整时间后重试。");
            return;
        }

        setEditingBusy(true);
        resultStatusLabel.setText("正在上传视频到后端...");

        ensureUploadedMedia().thenCompose(upload ->
                        apiClient.editMedia(
                                upload.mediaId,
                                start,
                                end,
                                speedSlider.getValue(),
                                effectSlider.getValue(),
                                demand,
                                autoVoiceCheck.isSelected()
                        ))
                .whenComplete((payload, error) -> Platform.runLater(() -> {
                    setEditingBusy(false);
                    if (error != null) {
                        String message = BackendApiClient.describeError(error);
                        resultStatusLabel.setText("生成失败：" + message);
                        AlimomentDialogs.showError(previewPane.getScene().getWindow(), "生成失败", message);
                        return;
                    }
                    renderEditResult(payload, start, end, demand);
                }));
    }

    private java.util.concurrent.CompletableFuture<BackendApiClient.UploadPayload> ensureUploadedMedia() {
        if (uploadedMediaId != null && !uploadedMediaId.isBlank() && selectedVideoFile != null) {
            BackendApiClient.UploadPayload payload = new BackendApiClient.UploadPayload();
            payload.mediaId = uploadedMediaId;
            payload.originalFilename = selectedVideoFile.getName();
            payload.filename = uploadedMediaId;
            payload.fileSize = selectedVideoFile.length();
            return java.util.concurrent.CompletableFuture.completedFuture(payload);
        }
        return apiClient.uploadMedia(selectedVideoFile).thenApply(payload -> {
            uploadedMediaId = payload.mediaId;
            return payload;
        });
    }

    private Duration[] resolveEditSegment() {
        double keep = trimSlider.getValue();
        double total = mediaPlayer != null && mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()
                ? mediaPlayer.getTotalDuration().toSeconds()
                : Math.max(keep, 1.0);

        double start;
        if (clipStart.greaterThan(Duration.ZERO)) {
            start = clipStart.toSeconds();
        } else if (mediaPlayer != null) {
            start = Math.max(0, mediaPlayer.getCurrentTime().toSeconds() - keep / 2.0);
        } else {
            start = 0;
        }

        double end = Math.min(total, start + keep);
        if (end <= start) {
            end = Math.min(total, start + 3.0);
        }
        return new Duration[]{Duration.seconds(start), Duration.seconds(end)};
    }

    private void renderEditResult(BackendApiClient.EditPayload payload, double start, double end, String demand) {
        String videoUrl = apiClient.resolveUrl(
                payload.outputRelativeUrl != null && !payload.outputRelativeUrl.isBlank() ? payload.outputRelativeUrl : payload.outputUrl
        );
        resultListBox.getChildren().clear();

        resultStatusLabel.setText(
                "生成完成：" + formatDuration(Duration.seconds(start)) + " - " + formatDuration(Duration.seconds(end))
                        + "，速度 x" + String.format(Locale.ROOT, "%.2f", payload.speed)
        );
        resultListBox.getChildren().add(resultStatusLabel);

        Label taskLabel = new Label("创作需求：" + shortDemand(demand));
        taskLabel.getStyleClass().add("smart-edit-status");
        resultListBox.getChildren().add(taskLabel);

        Label urlLabel = new Label("成片地址：" + videoUrl);
        urlLabel.getStyleClass().add("smart-edit-status");
        urlLabel.setWrapText(true);
        resultListBox.getChildren().add(urlLabel);

        if (payload.note != null && !payload.note.isBlank()) {
            Label noteLabel = new Label(payload.note);
            noteLabel.getStyleClass().add("smart-edit-status");
            noteLabel.setWrapText(true);
            resultListBox.getChildren().add(noteLabel);
        }

        loadVideo(URI.create(videoUrl));
        toolStatusLabel.setText("已生成成片，并切换到生成结果预览。");
    }

    private void setEditingBusy(boolean busy) {
        oneClickEditBtn.setDisable(busy);
        splitBtn.setDisable(busy);
        speedBtn.setDisable(busy);
        trimBtn.setDisable(busy);
        effectBtn.setDisable(busy);
        deleteBtn.setDisable(busy);
    }

    private static String shortDemand(String demand) {
        return demand.length() > 16 ? demand.substring(0, 16) + "..." : demand;
    }

    private static String formatDuration(Duration d) {
        int sec = (int) Math.floor(d.toSeconds());
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
    }

}
