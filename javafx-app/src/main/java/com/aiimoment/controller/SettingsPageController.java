package com.aiimoment.controller;

import com.aiimoment.AppTheme;
import com.aiimoment.Main;
import com.aiimoment.account.AccountScopedPreferences;
import com.aiimoment.account.AccountSectionBinder;
import com.aiimoment.account.AccountStore;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * 设置页：主题/开关 + 左侧目录与右侧滚动联动（无蓝条；高亮为静态样式）。
 */
public class SettingsPageController {

    private static final String KEY_FOLLOW_SYSTEM = "settings.follow_system_theme";
    private static final String KEY_AI_ASSIST = "settings.ai_assist";
    private static final String KEY_AUTO_EDIT = "settings.auto_edit";
    private static final String KEY_AUTO_SAVE = "settings.auto_save_enabled";
    private static final String KEY_AUTO_SAVE_MIN = "settings.auto_save_interval_min";
    private static final String KEY_LOCALE = "settings.ui_locale";
    private static final String KEY_SHORTCUT_SAVE = "settings.shortcut.save_project";
    private static final String KEY_SHORTCUT_UNDO = "settings.shortcut.undo";
    private static final String KEY_SHORTCUT_REDO = "settings.shortcut.redo";
    private static final String KEY_SHORTCUT_COPY = "settings.shortcut.copy";
    private static final String KEY_SHORTCUT_PASTE = "settings.shortcut.paste";
    private static final String KEY_SHORTCUT_CUT = "settings.shortcut.cut";
    private static final String KEY_SHORTCUT_SELECT_ALL = "settings.shortcut.select_all";
    private static final String KEY_SHORTCUT_OPEN_SETTINGS = "settings.shortcut.open_settings";
    private static final String KEY_SHORTCUT_QUIT = "settings.shortcut.quit";
    private static final String KEY_CACHE_MB = "settings.cache_display_mb";
    private static final String KEY_SAVE_PATH = "settings.save_path";
    private static final String KEY_AI_MODEL = "settings.ai_model_id";
    private static final String KEY_AUDIO_IN = "settings.audio_input";
    private static final String KEY_AUDIO_OUT = "settings.audio_output";
    private static final String KEY_EXPORT_PRESET = "settings.export_preset";
    private static final String KEY_AUTO_EDIT_THRESHOLD = "settings.auto_edit_threshold_sec";

    private static final Duration SWITCH_DURATION = Duration.millis(150);

    private static final ShortcutDef[] SHORTCUT_DEFS = {
            new ShortcutDef(KEY_SHORTCUT_SAVE, "保存项目", "Ctrl+S"),
            new ShortcutDef(KEY_SHORTCUT_UNDO, "撤销", "Ctrl+Z"),
            new ShortcutDef(KEY_SHORTCUT_REDO, "重做", "Ctrl+Y"),
            new ShortcutDef(KEY_SHORTCUT_COPY, "复制", "Ctrl+C"),
            new ShortcutDef(KEY_SHORTCUT_PASTE, "粘贴", "Ctrl+V"),
            new ShortcutDef(KEY_SHORTCUT_CUT, "剪切", "Ctrl+X"),
            new ShortcutDef(KEY_SHORTCUT_SELECT_ALL, "全选", "Ctrl+A"),
            new ShortcutDef(KEY_SHORTCUT_OPEN_SETTINGS, "打开设置", "Ctrl+COMMA"),
            new ShortcutDef(KEY_SHORTCUT_QUIT, "退出应用", "Ctrl+Q"),
    };

    private static final class ShortcutDef {
        final String prefKey;
        final String label;
        final String defaultCombo;

        ShortcutDef(String prefKey, String label, String defaultCombo) {
            this.prefKey = prefKey;
            this.label = label;
            this.defaultCombo = defaultCombo;
        }
    }
    private static final Color SWITCH_ON = Color.web("#2b8cff");
    private static final Color SWITCH_OFF = Color.web("#6f6f6f");
    private static final double SWITCH_W = 42;
    private static final double SWITCH_H = 22;
    private static final double KNOB_R = 8;
    private static final double KNOB_X = 10;

    private static final String SWATCH_ACTIVE = "theme-swatch-active";

    /** 与右侧锚点 VBox 的 id 一致（供 properties / 调试） */
    private static final String[] ANCHOR_IDS = {
            "setting-account-mgmt",
            "setting-theme-appearance",
            "setting-language-region",
            "setting-shortcuts",
            "setting-cache-mgmt",
            "setting-ai-assist",
            "setting-auto-edit",
            "setting-ai-model",
            "setting-voice-audio",
            "setting-save-path",
            "setting-auto-save",
            "setting-default-export"
    };

    private static final String DATA_ANCHOR_KEY = "data-anchor";

    private static final String ACTIVE_SIDE_CLASS = "settings-side-item-active";
    /** 程序化滚动 / 点击对齐：目标锚点距视口顶部的留白（与 IO rootMargin.top 一致） */
    private static final double SCROLL_TOP_OFFSET = 20.0;
    /** 手动滚动时判定「当前段」：等价于 rootMargin \"-20px 0 0 0\" — 取首个 relTop >= -SCROLL_TOP_OFFSET 的锚点 */
    private static final double NAV_ACTIVE_TOP_INSET = SCROLL_TOP_OFFSET;
    private static final double NAV_EDGE_TOP_EPS = 1.5;
    private static final double NAV_EDGE_BOTTOM_EPS = 8.0;
    private static final Duration SCROLL_ANIMATION_DURATION = Duration.millis(300);
    private static final Duration LISTENER_REENABLE_DELAY = Duration.millis(50);

    private static final double WHEEL_SCROLL_MULTIPLIER = 2.5;
    private static final double SIDE_NAV_WHEEL_SCROLL_MULTIPLIER = 2.5;

    private static final long SCROLL_HIGHLIGHT_THROTTLE_NS = 24_000_000L;

    @FXML
    private ScrollPane sideNavScrollPane;
    @FXML
    private VBox sideNavVBox;

    @FXML
    private ScrollPane settingsScrollPane;

    @FXML
    private Label navGroupGeneral;
    @FXML
    private Label navGroupAi;
    @FXML
    private Label navGroupExport;

    @FXML
    private Button navThemeAppearance;
    @FXML
    private Button navLanguageRegion;
    @FXML
    private Button navAccount;
    @FXML
    private Button navShortcuts;
    @FXML
    private Button navCache;
    @FXML
    private Button navAiAssist;
    @FXML
    private Button navAutoEdit;
    @FXML
    private Button navAiModel;
    @FXML
    private Button navVoiceAudio;
    @FXML
    private Button navSavePath;
    @FXML
    private Button navAutoSave;
    @FXML
    private Button navDefaultExport;

    @FXML
    private VBox anchorThemeAppearance;
    @FXML
    private VBox anchorLanguageRegion;
    @FXML
    private VBox anchorAccount;
    @FXML
    private VBox anchorShortcuts;
    @FXML
    private VBox anchorCache;
    @FXML
    private VBox anchorAiAssist;
    @FXML
    private VBox anchorAutoEdit;
    @FXML
    private VBox anchorAiModel;
    @FXML
    private VBox anchorVoiceAudio;
    @FXML
    private VBox anchorSavePath;
    @FXML
    private VBox anchorAutoSave;
    @FXML
    private VBox anchorDefaultExport;

    @FXML
    private StackPane themeDarkSwatch;
    @FXML
    private StackPane themeLightSwatch;
    @FXML
    private HBox themeModeModule;
    @FXML
    private ToggleButton followSystemThemeToggle;
    @FXML
    private ToggleButton aiAssistToggle;
    @FXML
    private ToggleButton autoEditToggle;
    @FXML
    private ToggleButton autoSaveToggle;

    @FXML
    private ComboBox<String> languageLocaleCombo;
    @FXML
    private VBox shortcutRowsVBox;
    @FXML
    private Hyperlink shortcutResetAllLink;
    @FXML
    private Label cacheSizeLabel;
    @FXML
    private Button cacheClearBtn;
    @FXML
    private HBox autoEditThresholdRow;
    @FXML
    private Spinner<Integer> autoEditThresholdSpinner;
    @FXML
    private ComboBox<String> aiModelCombo;
    @FXML
    private Button aiModelInfoBtn;
    @FXML
    private ComboBox<String> audioInputCombo;
    @FXML
    private ComboBox<String> audioOutputCombo;
    @FXML
    private Button audioInputTestBtn;
    @FXML
    private Button audioOutputTestBtn;
    @FXML
    private TextField savePathField;
    @FXML
    private Button savePathBrowseBtn;
    @FXML
    private Label savePathErrorLabel;
    @FXML
    private HBox autoSaveIntervalRow;
    @FXML
    private Spinner<Integer> autoSaveIntervalSpinner;
    @FXML
    private ComboBox<String> exportPresetCombo;
    @FXML
    private Label settingsToastLabel;

    @FXML
    private StackPane accountAvatarHitbox;
    @FXML
    private ImageView accountAvatarImage;
    @FXML
    private Label accountAvatarInitial;
    @FXML
    private Label accountSummaryNameLabel;
    @FXML
    private Label accountSummaryEmailLabel;
    @FXML
    private StackPane accountMenuHitbox;

    private AccountSectionBinder accountSectionBinder;

    private Button[] leafNavButtons;
    private VBox[] anchorNodes;
    private Timeline scrollTimeline;

    /** 程序化滚动（含 Timeline 动画）期间为 true：右侧 vvalue 不驱动左侧高亮 */
    private boolean suppressScrollDrivenHighlight;

    /**
     * 为 true 时，才根据右侧视口位置同步左侧高亮（滚轮/拖动滚动条会置 true）。
     * 点击左侧目录后为 false，避免程序化滚动过程中或结束后被视口计算覆盖。
     */
    private boolean viewportNavHighlightFollowsScroll = true;

    /** 平滑滚动动画结束时，将高亮恢复为该叶子索引（不依赖视口推算） */
    private int programmaticScrollHighlightTarget = -1;

    private Object navScrollLayoutSession = new Object();
    private Object scrollAnimSession = new Object();

    private long lastScrollHighlightNanos;

    /** 最近一次由「视口滚动」算出的高亮索引；无有效锚点相交时保留不改 */
    private int lastScrollDrivenHighlightIndex;

    private boolean syncingFromPrefs;
    private boolean shortcutRecording;
    private TextField shortcutRecordingField;
    private String shortcutRecordingPrefKey;
    private javafx.event.EventHandler<KeyEvent> shortcutSceneFilter;
    private final Map<String, TextField> shortcutFieldsByPrefKey = new LinkedHashMap<>();
    private PauseTransition toastHideDelay;

    @FXML
    public void initialize() {
        AccountStore.getInstance().load();
        AccountStore.getInstance().persistIfNew();
        AccountScopedPreferences.migrateLegacyIfEmpty(AccountStore.getInstance().getCurrentAccountId());

        leafNavButtons = new Button[]{
                navAccount, navThemeAppearance, navLanguageRegion, navShortcuts, navCache,
                navAiAssist, navAutoEdit, navAiModel, navVoiceAudio,
                navSavePath, navAutoSave, navDefaultExport
        };
        anchorNodes = new VBox[]{
                anchorAccount, anchorThemeAppearance, anchorLanguageRegion, anchorShortcuts, anchorCache,
                anchorAiAssist, anchorAutoEdit, anchorAiModel, anchorVoiceAudio,
                anchorSavePath, anchorAutoSave, anchorDefaultExport
        };

        wireGroupHeading(navGroupGeneral, anchorAccount);
        wireGroupHeading(navGroupAi, anchorAiAssist);
        wireGroupHeading(navGroupExport, anchorSavePath);

        for (int i = 0; i < leafNavButtons.length; i++) {
            final VBox target = anchorNodes[i];
            Button b = leafNavButtons[i];
            if (b != null && target != null) {
                b.setFocusTraversable(false);
                b.setDefaultButton(false);
                b.setCancelButton(false);
                b.setOnAction(e -> scrollToAnchor(target));
            }
        }

        for (int i = 0; i < anchorNodes.length && i < ANCHOR_IDS.length; i++) {
            if (anchorNodes[i] != null) {
                anchorNodes[i].getProperties().put(DATA_ANCHOR_KEY, ANCHOR_IDS[i]);
            }
        }

        if (sideNavScrollPane != null) {
            sideNavScrollPane.setPannable(false);
            sideNavScrollPane.setFitToWidth(true);
            installSideNavScrollWheelBoost();
        }

        if (settingsScrollPane != null) {
            settingsScrollPane.vvalueProperty().addListener((obs, o, n) -> onRightScrollValueMaybeThrottled());
            settingsScrollPane.viewportBoundsProperty().addListener((obs, o, n) -> requestThrottledScrollHighlightUpdate());
            Node content = settingsScrollPane.getContent();
            if (content instanceof Region) {
                ((Region) content).layoutBoundsProperty().addListener((obs, o, n) -> requestThrottledScrollHighlightUpdate());
            }
            installSettingsScrollWheelBoost();
            wireVerticalScrollbarMarksUserScroll();
        }

        setupThemeSwatches();
        setupAnimatedSwitches();
        setupPreferenceBoundModules();

        /* 双帧：首帧后 content 高度可靠，避免误判「已在底部」把最后一项高亮叠在首项上 */
        Platform.runLater(() -> {
            layoutContent();
            if (settingsScrollPane != null) {
                settingsScrollPane.setVvalue(0);
            }
            suppressScrollDrivenHighlight = false;
            setLeafHighlightIndex(0);
            lastScrollDrivenHighlightIndex = 0;
            Platform.runLater(() -> {
                layoutContent();
                viewportNavHighlightFollowsScroll = true;
                updateActiveNavFromScroll();
                updateThemeSwatchHighlight(AppTheme.readLight(currentProfilePrefs()));
                applyFollowSystemThemeState(followSystemThemeToggle != null && followSystemThemeToggle.isSelected());
            });
        });

        if (accountAvatarHitbox != null && accountSummaryNameLabel != null && accountSummaryEmailLabel != null
                && accountMenuHitbox != null) {
            accountSectionBinder = new AccountSectionBinder(
                    this,
                    accountAvatarHitbox,
                    accountAvatarImage,
                    accountAvatarInitial,
                    accountSummaryNameLabel,
                    accountSummaryEmailLabel,
                    accountMenuHitbox);
            accountSectionBinder.install();
        }
    }

    /** 当前登录账户对应的设置 Preferences 节点（主题、开关等）。 */
    public Preferences currentProfilePrefs() {
        return AccountScopedPreferences.forAccountId(AccountStore.getInstance().getCurrentAccountId());
    }

    /** 切换账户后刷新本页开关与主题，并写回全局主题缓存供对话框等使用。 */
    public void reloadPerAccountSettingsInUi() {
        String id = AccountStore.getInstance().getCurrentAccountId();
        AccountScopedPreferences.migrateLegacyIfEmpty(id);
        Preferences p = currentProfilePrefs();
        syncingFromPrefs = true;
        try {
            if (followSystemThemeToggle != null) {
                followSystemThemeToggle.setSelected(p.getBoolean(KEY_FOLLOW_SYSTEM, false));
            }
            if (aiAssistToggle != null) {
                aiAssistToggle.setSelected(p.getBoolean(KEY_AI_ASSIST, true));
            }
            if (autoEditToggle != null) {
                autoEditToggle.setSelected(p.getBoolean(KEY_AUTO_EDIT, true));
            }
            if (autoSaveToggle != null) {
                autoSaveToggle.setSelected(p.getBoolean(KEY_AUTO_SAVE, true));
            }
        } finally {
            syncingFromPrefs = false;
        }
        syncModuleFieldsFromProfile();
        applyAiAssistDependentState();
        updateAutoEditThresholdVisibility();
        updateAutoSaveIntervalVisibility();
        Scene scene = resolveScene();
        if (scene != null && scene.getRoot() instanceof StackPane) {
            boolean light = AppTheme.readLight(p);
            StackPane shell = (StackPane) scene.getRoot();
            AppTheme.apply(shell, scene, light, p, Preferences.userNodeForPackage(Main.class));
            updateThemeSwatchHighlight(light);
        }
        applyFollowSystemThemeState(followSystemThemeToggle != null && followSystemThemeToggle.isSelected());
        if (accountSectionBinder != null) {
            accountSectionBinder.refreshAll();
        }
    }

    /** 拖动右侧垂直滚动条视为用户手动滚动，恢复「视口驱动高亮」 */
    private void wireVerticalScrollbarMarksUserScroll() {
        if (settingsScrollPane == null) {
            return;
        }
        Runnable attach = () -> {
            Node n = settingsScrollPane.lookup(".scroll-bar:vertical");
            if (n instanceof ScrollBar) {
                ScrollBar sb = (ScrollBar) n;
                if (Boolean.TRUE.equals(sb.getProperties().get("aiimoment-nav-sync-wired"))) {
                    return;
                }
                sb.getProperties().put("aiimoment-nav-sync-wired", Boolean.TRUE);
                sb.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> viewportNavHighlightFollowsScroll = true);
                sb.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> viewportNavHighlightFollowsScroll = true);
            }
        };
        if (settingsScrollPane.getScene() != null) {
            Platform.runLater(attach);
        } else {
            settingsScrollPane.sceneProperty().addListener((obs, oldSc, newSc) -> {
                if (newSc != null) {
                    Platform.runLater(attach);
                }
            });
        }
    }

    private void wireGroupHeading(Label label, VBox firstAnchor) {
        if (label == null || firstAnchor == null) {
            return;
        }
        label.setCursor(Cursor.HAND);
        label.setOnMouseClicked(e -> scrollToAnchor(firstAnchor));
    }

    private void layoutContent() {
        if (settingsScrollPane == null) {
            return;
        }
        Node content = settingsScrollPane.getContent();
        if (content != null) {
            content.applyCss();
        }
    }

    private static double measurableContentHeight(Region content) {
        double layoutH = content.getLayoutBounds().getHeight();
        double localH = content.getBoundsInLocal().getHeight();
        return Math.max(layoutH, localH);
    }

    private static double rightSettingsScrollableHeight(Region content) {
        double base = measurableContentHeight(content);
        if (!(content instanceof VBox)) {
            return base;
        }
        VBox v = (VBox) content;
        double maxBottom = 0;
        for (Node n : v.getChildren()) {
            if (!n.isManaged()) {
                continue;
            }
            maxBottom = Math.max(maxBottom, n.getBoundsInParent().getMaxY());
        }
        return Math.max(base, maxBottom);
    }

    private static double readSettingsScrollPixelY(Region content) {
        Bounds cb = content.getBoundsInParent();
        return Math.max(0, -cb.getMinY());
    }

    private static double settingsScrollPixelRange(ScrollPane sp, Region content, double contentH, double viewportH) {
        double fromLayout = Math.max(0, contentH - viewportH);
        double v = sp.getVvalue();
        double scrollPx = readSettingsScrollPixelY(content);
        if (v > 0.04 && scrollPx > 2) {
            double implied = scrollPx / v;
            return Math.max(fromLayout, implied);
        }
        return fromLayout;
    }

    private static double anchorTopInContent(Node anchor, Region content) {
        if (anchor == null || content == null) {
            return 0;
        }
        if (anchor.getParent() == content) {
            return anchor.getBoundsInParent().getMinY();
        }
        double y = 0;
        Node n = anchor;
        while (n != null && n != content) {
            y += n.getLayoutY() + n.getTranslateY();
            n = n.getParent();
        }
        if (n == content) {
            return y;
        }
        return anchor.getBoundsInParent().getMinY();
    }

    private static double anchorBottomInContent(Node anchor, Region content) {
        if (anchor == null || content == null) {
            return 0;
        }
        if (anchor.getParent() == content) {
            return anchor.getBoundsInParent().getMaxY();
        }
        double top = anchorTopInContent(anchor, content);
        double h = Math.max(1, anchor.getBoundsInLocal().getHeight());
        return top + h;
    }

    /** 锚点与右侧视口在垂直方向是否有交集（用于「空白区域保持上次高亮」） */
    private static boolean anchorIntersectsViewport(VBox anchor, Region content, double scrollY, double viewportH) {
        if (anchor == null || viewportH <= 0) {
            return false;
        }
        double top = anchorTopInContent(anchor, content);
        double bottom = anchorBottomInContent(anchor, content);
        return bottom > scrollY && top < scrollY + viewportH;
    }

    private void installSideNavScrollWheelBoost() {
        sideNavScrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0) {
                return;
            }
            Region content = (Region) sideNavScrollPane.getContent();
            if (content == null) {
                return;
            }
            double contentH = measurableContentHeight(content);
            double viewportH = sideNavScrollPane.getViewportBounds().getHeight();
            double maxScroll = Math.max(0, contentH - viewportH);
            if (maxScroll <= 1e-3) {
                return;
            }
            e.consume();
            double pixelDelta = -e.getDeltaY() * SIDE_NAV_WHEEL_SCROLL_MULTIPLIER;
            double deltaV = pixelDelta / maxScroll;
            sideNavScrollPane.setVvalue(clamp(sideNavScrollPane.getVvalue() + deltaV, 0, 1));
        });
    }

    private void installSettingsScrollWheelBoost() {
        settingsScrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0) {
                return;
            }
            viewportNavHighlightFollowsScroll = true;
            Region content = (Region) settingsScrollPane.getContent();
            if (content == null) {
                return;
            }
            double contentH = rightSettingsScrollableHeight(content);
            double viewportH = settingsScrollPane.getViewportBounds().getHeight();
            double maxScroll = settingsScrollPixelRange(settingsScrollPane, content, contentH, viewportH);
            if (maxScroll <= 1e-3) {
                return;
            }
            e.consume();
            double pixelDelta = -e.getDeltaY() * WHEEL_SCROLL_MULTIPLIER;
            double deltaV = pixelDelta / maxScroll;
            settingsScrollPane.setVvalue(clamp(settingsScrollPane.getVvalue() + deltaV, 0, 1));
        });
    }

    private int indexForAnchor(VBox anchor) {
        for (int i = 0; i < anchorNodes.length; i++) {
            if (anchorNodes[i] == anchor) {
                return i;
            }
        }
        return -1;
    }

    private void stopScrollAnimationSilently() {
        if (scrollTimeline != null) {
            scrollTimeline.stop();
            scrollTimeline.setOnFinished(null);
            scrollTimeline = null;
        }
    }

    private void onRightScrollValueMaybeThrottled() {
        requestThrottledScrollHighlightUpdate();
    }

    /** vvalue / 视口尺寸 / 内容布局变化时统一走节流，避免高频重算左侧高亮 */
    private void requestThrottledScrollHighlightUpdate() {
        if (suppressScrollDrivenHighlight || !viewportNavHighlightFollowsScroll) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastScrollHighlightNanos < SCROLL_HIGHLIGHT_THROTTLE_NS) {
            return;
        }
        lastScrollHighlightNanos = now;
        updateActiveNavFromScroll();
    }

    /**
     * 仅通过 {@link #ACTIVE_SIDE_CLASS} 表示选中；先清空所有叶子项上的该类（含重复），再赋给唯一一项。
     */
    private void setLeafHighlightIndex(int index) {
        for (Button b : leafNavButtons) {
            if (b == null) {
                continue;
            }
            while (b.getStyleClass().remove(ACTIVE_SIDE_CLASS)) {
                /* 移除所有重复项，避免叠层高亮 */
            }
        }
        if (index >= 0 && index < leafNavButtons.length && leafNavButtons[index] != null) {
            leafNavButtons[index].getStyleClass().add(ACTIVE_SIDE_CLASS);
        }
    }

    /**
     * 结束「仅抑制程序化滚动」阶段，保持用户点击项高亮，不根据视口重算（避免跳回首/末项）。
     */
    private void scheduleReenableAfterProgrammaticScroll(int leafIndex) {
        PauseTransition pause = new PauseTransition(LISTENER_REENABLE_DELAY);
        pause.setOnFinished(e -> {
            suppressScrollDrivenHighlight = false;
            if (leafIndex >= 0 && leafIndex < leafNavButtons.length) {
                setLeafHighlightIndex(leafIndex);
            }
        });
        pause.play();
    }

    private void finishProgrammaticScroll(Object animSession) {
        if (scrollAnimSession != animSession) {
            return;
        }
        suppressScrollDrivenHighlight = false;
        int t = programmaticScrollHighlightTarget;
        programmaticScrollHighlightTarget = -1;
        if (t >= 0 && t < leafNavButtons.length) {
            setLeafHighlightIndex(t);
        }
    }

    private void scrollToAnchor(VBox anchor) {
        if (settingsScrollPane == null || anchor == null) {
            return;
        }
        int clickedIdx = indexForAnchor(anchor);
        if (clickedIdx < 0) {
            return;
        }
        setLeafHighlightIndex(clickedIdx);
        viewportNavHighlightFollowsScroll = false;

        final Object layoutSession = new Object();
        navScrollLayoutSession = layoutSession;

        stopScrollAnimationSilently();
        programmaticScrollHighlightTarget = -1;

        suppressScrollDrivenHighlight = true;
        layoutContent();
        settingsScrollPane.requestLayout();
        Node rawContent = settingsScrollPane.getContent();
        if (rawContent instanceof Region) {
            ((Region) rawContent).requestLayout();
        }

        Platform.runLater(() -> Platform.runLater(() -> {
            if (navScrollLayoutSession != layoutSession) {
                return;
            }
            scrollToAnchorAfterLayout(anchor, clickedIdx, layoutSession);
        }));
    }

    private void scrollToAnchorAfterLayout(VBox anchor, int clickedLeafIndex, Object layoutSession) {
        if (settingsScrollPane == null || anchor == null || navScrollLayoutSession != layoutSession) {
            return;
        }
        Region content = (Region) settingsScrollPane.getContent();
        if (content == null) {
            suppressScrollDrivenHighlight = false;
            viewportNavHighlightFollowsScroll = true;
            return;
        }
        content.applyCss();

        double viewportH = settingsScrollPane.getViewportBounds().getHeight();
        double contentH = rightSettingsScrollableHeight(content);
        double maxScroll = settingsScrollPixelRange(settingsScrollPane, content, contentH, viewportH);

        if (maxScroll <= 1e-3) {
            settingsScrollPane.setVvalue(0);
            setLeafHighlightIndex(clickedLeafIndex);
            scheduleReenableAfterProgrammaticScroll(clickedLeafIndex);
            return;
        }

        double anchorTop = anchorTopInContent(anchor, content);
        double targetY = clamp(anchorTop - SCROLL_TOP_OFFSET, 0, maxScroll);
        double scrollY = readSettingsScrollPixelY(content);

        final double EDGE_EPS = 2.0;
        final double ALIGNED_EPS = 4.0;
        boolean needScrollUp = targetY < scrollY - EDGE_EPS;
        boolean needScrollDown = targetY > scrollY + EDGE_EPS;

        /* 已在目标位置附近：不滚动，仅高亮点击项后恢复监听 */
        if (Math.abs(targetY - scrollY) < ALIGNED_EPS) {
            setLeafHighlightIndex(clickedLeafIndex);
            scheduleReenableAfterProgrammaticScroll(clickedLeafIndex);
            return;
        }

        /* 已在顶部且目标在视口上方（需向上滚但 scrollTop 已为 0）：不滚动，仅高亮 */
        if (scrollY <= EDGE_EPS && needScrollUp) {
            setLeafHighlightIndex(clickedLeafIndex);
            scheduleReenableAfterProgrammaticScroll(clickedLeafIndex);
            return;
        }

        /* 已在底部且目标在视口下方（需向下滚但 scrollTop 已为 max）：不滚动，仅高亮 */
        if (scrollY >= maxScroll - EDGE_EPS && needScrollDown) {
            setLeafHighlightIndex(clickedLeafIndex);
            scheduleReenableAfterProgrammaticScroll(clickedLeafIndex);
            return;
        }

        double targetV = maxScroll > 1e-6 ? targetY / maxScroll : 0;

        stopScrollAnimationSilently();
        final Object animSession = new Object();
        scrollAnimSession = animSession;
        programmaticScrollHighlightTarget = clickedLeafIndex;

        scrollTimeline = new Timeline(
                new KeyFrame(SCROLL_ANIMATION_DURATION,
                        new KeyValue(settingsScrollPane.vvalueProperty(), clamp(targetV, 0, 1), Interpolator.EASE_OUT))
        );
        scrollTimeline.setOnFinished(ev -> finishProgrammaticScroll(animSession));
        scrollTimeline.play();
    }

    /**
     * 根据右侧 ScrollPane 视口位置更新左侧高亮（无动画，仅样式类）。
     * <ul>
     *   <li>顶边：scrollTop≈0 → 高亮第一项</li>
     *   <li>底边：scrollTop≈max 或 vvalue≈1 → 高亮最后一项</li>
     *   <li>中间：等价 IO rootMargin.top=-20px，取首个 relTop &gt;= -{@link #NAV_ACTIVE_TOP_INSET} 的锚点</li>
     *   <li>无任何锚点与视口相交时：保持 {@link #lastScrollDrivenHighlightIndex} 不变</li>
     * </ul>
     */
    private void updateActiveNavFromScroll() {
        if (settingsScrollPane == null) {
            return;
        }
        if (suppressScrollDrivenHighlight) {
            return;
        }
        if (!viewportNavHighlightFollowsScroll) {
            return;
        }
        Region content = (Region) settingsScrollPane.getContent();
        if (content == null) {
            return;
        }

        double viewportH = settingsScrollPane.getViewportBounds().getHeight();
        if (viewportH <= 0) {
            return;
        }

        double contentH = rightSettingsScrollableHeight(content);
        double maxScroll = settingsScrollPixelRange(settingsScrollPane, content, contentH, viewportH);
        double scrollY = readSettingsScrollPixelY(content);

        if (maxScroll <= 1e-3) {
            lastScrollDrivenHighlightIndex = 0;
            setLeafHighlightIndex(0);
            return;
        }

        boolean anyIntersect = false;
        for (VBox a : anchorNodes) {
            if (a != null && anchorIntersectsViewport(a, content, scrollY, viewportH)) {
                anyIntersect = true;
                break;
            }
        }
        if (!anyIntersect) {
            return;
        }

        double v = settingsScrollPane.getVvalue();
        int n = leafNavButtons.length;
        int active;

        if (scrollY <= NAV_EDGE_TOP_EPS) {
            active = 0;
        } else if (v >= 1.0 - 1e-4 || scrollY >= maxScroll - NAV_EDGE_BOTTOM_EPS) {
            active = n - 1;
        } else {
            active = lastScrollDrivenHighlightIndex;
            boolean found = false;
            for (int i = 0; i < anchorNodes.length; i++) {
                VBox a = anchorNodes[i];
                if (a == null) {
                    continue;
                }
                double relTop = anchorTopInContent(a, content) - scrollY;
                if (relTop >= -NAV_ACTIVE_TOP_INSET) {
                    active = i;
                    found = true;
                    break;
                }
            }
            if (!found) {
                active = clampInt(lastScrollDrivenHighlightIndex, 0, n - 1);
            }
        }

        lastScrollDrivenHighlightIndex = active;
        setLeafHighlightIndex(active);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void setupThemeSwatches() {
        if (themeDarkSwatch != null) {
            themeDarkSwatch.setOnMouseClicked(e -> applyThemeChoice(false));
        }
        if (themeLightSwatch != null) {
            themeLightSwatch.setOnMouseClicked(e -> applyThemeChoice(true));
        }
    }

    private void applyThemeChoice(boolean light) {
        Scene scene = resolveScene();
        if (scene == null || !(scene.getRoot() instanceof StackPane)) {
            return;
        }
        StackPane shell = (StackPane) scene.getRoot();
        AppTheme.apply(shell, scene, light, currentProfilePrefs(), Preferences.userNodeForPackage(Main.class));
        updateThemeSwatchHighlight(light);
        if (!syncingFromPrefs) {
            showSavedToast();
        }
    }

    private Scene resolveScene() {
        if (themeDarkSwatch != null && themeDarkSwatch.getScene() != null) {
            return themeDarkSwatch.getScene();
        }
        if (themeLightSwatch != null && themeLightSwatch.getScene() != null) {
            return themeLightSwatch.getScene();
        }
        if (settingsScrollPane != null && settingsScrollPane.getScene() != null) {
            return settingsScrollPane.getScene();
        }
        if (followSystemThemeToggle != null) {
            return followSystemThemeToggle.getScene();
        }
        return null;
    }

    private void updateThemeSwatchHighlight(boolean light) {
        if (themeDarkSwatch != null) {
            themeDarkSwatch.getStyleClass().remove(SWATCH_ACTIVE);
            if (!light) {
                themeDarkSwatch.getStyleClass().add(SWATCH_ACTIVE);
            }
        }
        if (themeLightSwatch != null) {
            themeLightSwatch.getStyleClass().remove(SWATCH_ACTIVE);
            if (light) {
                themeLightSwatch.getStyleClass().add(SWATCH_ACTIVE);
            }
        }
    }

    private void setupAnimatedSwitches() {
        if (followSystemThemeToggle != null) {
            boolean selected = currentProfilePrefs().getBoolean(KEY_FOLLOW_SYSTEM, false);
            setupAnimatedSwitch(followSystemThemeToggle, selected, on -> {
                currentProfilePrefs().putBoolean(KEY_FOLLOW_SYSTEM, on);
                applyFollowSystemThemeState(on);
                if (!syncingFromPrefs) {
                    showSavedToast();
                }
            });
        }
        if (aiAssistToggle != null) {
            boolean selected = currentProfilePrefs().getBoolean(KEY_AI_ASSIST, true);
            setupAnimatedSwitch(aiAssistToggle, selected, on -> {
                currentProfilePrefs().putBoolean(KEY_AI_ASSIST, on);
                applyAiAssistDependentState();
                if (!syncingFromPrefs) {
                    showSavedToast();
                }
            });
        }
        if (autoEditToggle != null) {
            boolean selected = currentProfilePrefs().getBoolean(KEY_AUTO_EDIT, true);
            setupAnimatedSwitch(autoEditToggle, selected, on -> {
                currentProfilePrefs().putBoolean(KEY_AUTO_EDIT, on);
                updateAutoEditThresholdVisibility();
                if (!syncingFromPrefs) {
                    showSavedToast();
                }
            });
        }
        if (autoSaveToggle != null) {
            boolean selected = currentProfilePrefs().getBoolean(KEY_AUTO_SAVE, true);
            setupAnimatedSwitch(autoSaveToggle, selected, on -> {
                currentProfilePrefs().putBoolean(KEY_AUTO_SAVE, on);
                updateAutoSaveIntervalVisibility();
                if (!syncingFromPrefs) {
                    showSavedToast();
                }
            });
        }
    }

    private void setupPreferenceBoundModules() {
        setupLanguageLocaleCombo();
        setupShortcutsModule();
        setupCacheRow();
        setupAutoEditSpinner();
        setupAiModelRow();
        setupAudioRows();
        setupSavePathRow();
        setupAutoSaveSpinner();
        setupExportPresetCombo();
        syncModuleFieldsFromProfile();
        applyAiAssistDependentState();
        updateAutoEditThresholdVisibility();
        updateAutoSaveIntervalVisibility();
    }

    private void syncModuleFieldsFromProfile() {
        syncingFromPrefs = true;
        try {
            Preferences p = currentProfilePrefs();
            if (languageLocaleCombo != null) {
                String loc = p.get(KEY_LOCALE, "简体中文（中国）");
                languageLocaleCombo.getSelectionModel().select(loc);
            }
            for (ShortcutDef def : SHORTCUT_DEFS) {
                TextField tf = shortcutFieldsByPrefKey.get(def.prefKey);
                if (tf != null) {
                    tf.setText(p.get(def.prefKey, def.defaultCombo));
                }
            }
            refreshCacheSizeLabel();
            if (autoEditThresholdSpinner != null) {
                int sec = p.getInt(KEY_AUTO_EDIT_THRESHOLD, 30);
                autoEditThresholdSpinner.getValueFactory().setValue(clampInt(sec, 5, 600));
            }
            if (aiModelCombo != null) {
                String mid = p.get(KEY_AI_MODEL, aiModelCombo.getItems().get(0));
                if (aiModelCombo.getItems().contains(mid)) {
                    aiModelCombo.getSelectionModel().select(mid);
                } else {
                    aiModelCombo.getSelectionModel().selectFirst();
                }
            }
            fillAudioComboSelections();
            if (savePathField != null) {
                savePathField.setText(p.get(KEY_SAVE_PATH, defaultSavePath()));
            }
            hideSavePathError();
            if (autoSaveIntervalSpinner != null) {
                int min = p.getInt(KEY_AUTO_SAVE_MIN, 5);
                autoSaveIntervalSpinner.getValueFactory().setValue(clampInt(min, 1, 120));
            }
            if (exportPresetCombo != null) {
                String ex = p.get(KEY_EXPORT_PRESET, exportPresetCombo.getItems().get(0));
                if (exportPresetCombo.getItems().contains(ex)) {
                    exportPresetCombo.getSelectionModel().select(ex);
                } else {
                    exportPresetCombo.getSelectionModel().selectFirst();
                }
            }
        } finally {
            syncingFromPrefs = false;
        }
    }

    private static int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String defaultSavePath() {
        return Path.of(System.getProperty("user.home"), "Documents", "AIiMoment").toString();
    }

    private void applyAiAssistDependentState() {
        boolean on = aiAssistToggle == null || aiAssistToggle.isSelected();
        if (anchorAutoEdit != null) {
            anchorAutoEdit.setDisable(!on);
        }
        if (anchorAiModel != null) {
            anchorAiModel.setDisable(!on);
        }
        if (anchorVoiceAudio != null) {
            anchorVoiceAudio.setDisable(!on);
        }
    }

    private void updateAutoEditThresholdVisibility() {
        boolean on = autoEditToggle != null && autoEditToggle.isSelected();
        if (autoEditThresholdRow != null) {
            autoEditThresholdRow.setVisible(on);
            autoEditThresholdRow.setManaged(on);
        }
    }

    private void updateAutoSaveIntervalVisibility() {
        boolean on = autoSaveToggle != null && autoSaveToggle.isSelected();
        if (autoSaveIntervalRow != null) {
            autoSaveIntervalRow.setVisible(on);
            autoSaveIntervalRow.setManaged(on);
        }
    }

    private void setupLanguageLocaleCombo() {
        if (languageLocaleCombo == null) {
            return;
        }
        languageLocaleCombo.setItems(FXCollections.observableArrayList(
                "简体中文（中国）",
                "English (United States)",
                "日本語（日本）"
        ));
        languageLocaleCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (syncingFromPrefs || b == null) {
                return;
            }
            currentProfilePrefs().put(KEY_LOCALE, b);
            showSavedToast();
        });
    }

    private void setupShortcutsModule() {
        if (shortcutRowsVBox == null) {
            return;
        }
        shortcutRowsVBox.getChildren().clear();
        shortcutFieldsByPrefKey.clear();
        for (ShortcutDef def : SHORTCUT_DEFS) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("settings-form-row");
            Label lb = new Label(def.label);
            lb.getStyleClass().add("settings-field-label");
            lb.setMinWidth(140);
            TextField tf = new TextField();
            tf.setEditable(false);
            tf.setFocusTraversable(true);
            tf.setPrefWidth(200);
            tf.getStyleClass().add("settings-key-field");
            tf.setOnMouseClicked(ev -> startShortcutRecordingForField(tf, def.prefKey));
            HBox.setHgrow(tf, Priority.ALWAYS);
            row.getChildren().addAll(lb, tf);
            shortcutRowsVBox.getChildren().add(row);
            shortcutFieldsByPrefKey.put(def.prefKey, tf);
        }
        if (shortcutResetAllLink != null) {
            shortcutResetAllLink.setOnAction(e -> restoreAllShortcutDefaults());
        }
    }

    private void restoreAllShortcutDefaults() {
        Preferences p = currentProfilePrefs();
        for (ShortcutDef def : SHORTCUT_DEFS) {
            p.put(def.prefKey, def.defaultCombo);
            TextField tf = shortcutFieldsByPrefKey.get(def.prefKey);
            if (tf != null) {
                tf.setText(def.defaultCombo);
            }
        }
        showSavedToast();
    }

    private void startShortcutRecordingForField(TextField field, String prefKey) {
        if (field == null) {
            return;
        }
        Scene sc = field.getScene();
        if (sc == null) {
            return;
        }
        if (shortcutRecording) {
            if (shortcutRecordingField == field) {
                endShortcutRecording(sc, true);
                return;
            }
            endShortcutRecording(sc, true);
        }
        shortcutRecording = true;
        shortcutRecordingField = field;
        shortcutRecordingPrefKey = prefKey;
        for (TextField f : shortcutFieldsByPrefKey.values()) {
            f.getStyleClass().remove("settings-key-field-recording");
        }
        field.getStyleClass().add("settings-key-field-recording");
        shortcutSceneFilter = e -> {
            if (!shortcutRecording) {
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                endShortcutRecording(sc, false);
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ENTER) {
                endShortcutRecording(sc, true);
                e.consume();
                return;
            }
            if (e.getEventType() == KeyEvent.KEY_PRESSED) {
                String combo = formatKeyCombo(e);
                if (combo != null && shortcutRecordingField != null) {
                    shortcutRecordingField.setText(combo);
                    e.consume();
                }
            }
        };
        sc.addEventFilter(KeyEvent.ANY, shortcutSceneFilter);
    }

    private void endShortcutRecording(Scene sc, boolean applySave) {
        if (!shortcutRecording) {
            return;
        }
        shortcutRecording = false;
        TextField field = shortcutRecordingField;
        String key = shortcutRecordingPrefKey;
        for (TextField f : shortcutFieldsByPrefKey.values()) {
            f.getStyleClass().remove("settings-key-field-recording");
        }
        if (shortcutSceneFilter != null && sc != null) {
            sc.removeEventFilter(KeyEvent.ANY, shortcutSceneFilter);
            shortcutSceneFilter = null;
        }
        shortcutRecordingField = null;
        shortcutRecordingPrefKey = null;
        if (field != null && key != null) {
            if (applySave) {
                currentProfilePrefs().put(key, field.getText());
                showSavedToast();
            } else {
                Preferences p = currentProfilePrefs();
                for (ShortcutDef d : SHORTCUT_DEFS) {
                    if (d.prefKey.equals(key)) {
                        field.setText(p.get(d.prefKey, d.defaultCombo));
                        break;
                    }
                }
            }
        }
    }

    private static String formatKeyCombo(KeyEvent e) {
        KeyCode c = e.getCode();
        if (c == null) {
            return null;
        }
        if (c == KeyCode.CONTROL || c == KeyCode.ALT || c == KeyCode.SHIFT || c == KeyCode.META
                || c == KeyCode.WINDOWS || c == KeyCode.COMMAND || c == KeyCode.UNDEFINED) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (e.isControlDown()) {
            sb.append("Ctrl+");
        }
        if (e.isAltDown()) {
            sb.append("Alt+");
        }
        if (e.isShiftDown()) {
            sb.append("Shift+");
        }
        if (e.isMetaDown()) {
            sb.append("Meta+");
        }
        sb.append(c.getName());
        return sb.toString();
    }

    private void setupCacheRow() {
        if (cacheClearBtn == null) {
            return;
        }
        cacheClearBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            if (cacheClearBtn.getScene() != null && cacheClearBtn.getScene().getWindow() != null) {
                a.initOwner(cacheClearBtn.getScene().getWindow());
            }
            a.setTitle("清除缓存");
            a.setHeaderText(null);
            a.setContentText("确定清除所有缓存？此操作不可撤销。");
            Optional<ButtonType> r = a.showAndWait();
            if (r.isPresent() && r.get() == ButtonType.OK) {
                currentProfilePrefs().putInt(KEY_CACHE_MB, 0);
                refreshCacheSizeLabel();
                showToast("缓存已清除");
            }
        });
    }

    private void refreshCacheSizeLabel() {
        if (cacheSizeLabel == null) {
            return;
        }
        int mb = currentProfilePrefs().getInt(KEY_CACHE_MB, 256);
        cacheSizeLabel.setText(mb + " MB");
    }

    private void setupAutoEditSpinner() {
        if (autoEditThresholdSpinner == null) {
            return;
        }
        autoEditThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 600, 30, 1));
        autoEditThresholdSpinner.valueProperty().addListener((o, a, b) -> {
            if (syncingFromPrefs || b == null) {
                return;
            }
            currentProfilePrefs().putInt(KEY_AUTO_EDIT_THRESHOLD, b);
            showSavedToast();
        });
    }

    private void setupAiModelRow() {
        if (aiModelCombo == null) {
            return;
        }
        aiModelCombo.setItems(FXCollections.observableArrayList(
                "通用模型 v2",
                "快速模型 v1",
                "高精度模型 beta"
        ));
        aiModelCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (syncingFromPrefs || b == null) {
                return;
            }
            currentProfilePrefs().put(KEY_AI_MODEL, b);
            showSavedToast();
        });
        if (aiModelInfoBtn != null) {
            aiModelInfoBtn.setOnAction(e -> {
                String m = aiModelCombo.getSelectionModel().getSelectedItem();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                if (aiModelInfoBtn.getScene() != null && aiModelInfoBtn.getScene().getWindow() != null) {
                    info.initOwner(aiModelInfoBtn.getScene().getWindow());
                }
                info.setTitle("模型信息");
                info.setHeaderText(m != null ? m : "模型");
                info.setContentText("版本与精度说明为占位内容。切换模型后若需下载较大文件，将显示进度（后续版本）。");
                info.showAndWait();
            });
        }
    }

    private void setupAudioRows() {
        if (audioInputCombo != null) {
            audioInputCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
                if (syncingFromPrefs || b == null) {
                    return;
                }
                currentProfilePrefs().put(KEY_AUDIO_IN, b);
                showSavedToast();
            });
        }
        if (audioOutputCombo != null) {
            audioOutputCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
                if (syncingFromPrefs || b == null) {
                    return;
                }
                currentProfilePrefs().put(KEY_AUDIO_OUT, b);
                showSavedToast();
            });
        }
        if (audioOutputTestBtn != null) {
            audioOutputTestBtn.setOnAction(e -> playTestBeep());
        }
        if (audioInputTestBtn != null) {
            audioInputTestBtn.setOnAction(e -> {
                String name = audioInputCombo != null ? audioInputCombo.getSelectionModel().getSelectedItem() : "—";
                showToast("已选择输入设备：" + name);
            });
        }
    }

    private void fillAudioComboSelections() {
        List<String> inDevs = listInputDeviceNames();
        List<String> outDevs = listOutputDeviceNames();
        if (audioInputCombo != null) {
            String cur = currentProfilePrefs().get(KEY_AUDIO_IN, inDevs.get(0));
            audioInputCombo.setItems(FXCollections.observableArrayList(inDevs));
            if (inDevs.contains(cur)) {
                audioInputCombo.getSelectionModel().select(cur);
            } else {
                audioInputCombo.getSelectionModel().selectFirst();
            }
        }
        if (audioOutputCombo != null) {
            String cur = currentProfilePrefs().get(KEY_AUDIO_OUT, outDevs.get(0));
            audioOutputCombo.setItems(FXCollections.observableArrayList(outDevs));
            if (outDevs.contains(cur)) {
                audioOutputCombo.getSelectionModel().select(cur);
            } else {
                audioOutputCombo.getSelectionModel().selectFirst();
            }
        }
    }

    private static List<String> listInputDeviceNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("系统默认");
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                Mixer m = AudioSystem.getMixer(info);
                if (m.isLineSupported(new Line.Info(TargetDataLine.class))) {
                    names.add(info.getName());
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return new ArrayList<>(names);
    }

    private static List<String> listOutputDeviceNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("系统默认");
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                Mixer m = AudioSystem.getMixer(info);
                if (m.isLineSupported(new Line.Info(SourceDataLine.class))) {
                    names.add(info.getName());
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return new ArrayList<>(names);
    }

    private static void playTestBeep() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {
            /* headless */
        }
    }

    private void setupSavePathRow() {
        if (savePathBrowseBtn == null || savePathField == null) {
            return;
        }
        savePathBrowseBtn.setOnAction(e -> {
            DirectoryChooser ch = new DirectoryChooser();
            ch.setTitle("选择默认保存目录");
            File init = new File(savePathField.getText());
            if (init.isDirectory()) {
                ch.setInitialDirectory(init);
            }
            File dir = ch.showDialog(savePathBrowseBtn.getScene().getWindow());
            if (dir != null) {
                savePathField.setText(dir.getAbsolutePath());
                validateAndPersistSavePath(dir.toPath());
            }
        });
    }

    private void validateAndPersistSavePath(Path path) {
        hideSavePathError();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            if (!Files.isWritable(path)) {
                showSavePathError("该目录无写入权限，请选择其他路径。");
                return;
            }
        } catch (Exception ex) {
            showSavePathError("路径无效或无法创建目录。");
            return;
        }
        currentProfilePrefs().put(KEY_SAVE_PATH, path.toAbsolutePath().toString());
        showSavedToast();
    }

    private void showSavePathError(String msg) {
        if (savePathErrorLabel != null) {
            savePathErrorLabel.setText(msg);
            savePathErrorLabel.setVisible(true);
            savePathErrorLabel.setManaged(true);
        }
    }

    private void hideSavePathError() {
        if (savePathErrorLabel != null) {
            savePathErrorLabel.setVisible(false);
            savePathErrorLabel.setManaged(false);
        }
    }

    private void setupAutoSaveSpinner() {
        if (autoSaveIntervalSpinner == null) {
            return;
        }
        autoSaveIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 5, 1));
        autoSaveIntervalSpinner.valueProperty().addListener((o, a, b) -> {
            if (syncingFromPrefs || b == null) {
                return;
            }
            currentProfilePrefs().putInt(KEY_AUTO_SAVE_MIN, b);
            showSavedToast();
        });
    }

    private void setupExportPresetCombo() {
        if (exportPresetCombo == null) {
            return;
        }
        exportPresetCombo.setItems(FXCollections.observableArrayList(
                "MP4 1080p",
                "MP4 720p",
                "GIF 中等质量",
                "MOV 原始",
                "音频 MP3"
        ));
        exportPresetCombo.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (syncingFromPrefs || b == null) {
                return;
            }
            currentProfilePrefs().put(KEY_EXPORT_PRESET, b);
            showSavedToast();
        });
    }

    private void showSavedToast() {
        showToast("已保存");
    }

    private void showToast(String message) {
        if (settingsToastLabel == null) {
            return;
        }
        settingsToastLabel.setText(message);
        settingsToastLabel.setVisible(true);
        settingsToastLabel.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(120), settingsToastLabel);
        in.setToValue(1);
        in.play();
        if (toastHideDelay != null) {
            toastHideDelay.stop();
        }
        toastHideDelay = new PauseTransition(Duration.seconds(1.6));
        toastHideDelay.setOnFinished(ev -> {
            FadeTransition out = new FadeTransition(Duration.millis(220), settingsToastLabel);
            out.setFromValue(1);
            out.setToValue(0);
            out.setOnFinished(e2 -> settingsToastLabel.setVisible(false));
            out.play();
        });
        toastHideDelay.play();
    }

    private void applyFollowSystemThemeState(boolean followSystem) {
        if (themeModeModule != null) {
            themeModeModule.setDisable(followSystem);
        }
    }

    private void setupAnimatedSwitch(ToggleButton button, boolean selected, Consumer<Boolean> onChange) {
        button.setFocusTraversable(false);
        button.setText("");
        button.getStyleClass().add("switch-toggle-animated");

        Rectangle track = new Rectangle(SWITCH_W, SWITCH_H);
        track.setArcWidth(SWITCH_H);
        track.setArcHeight(SWITCH_H);
        track.setFill(selected ? SWITCH_ON : SWITCH_OFF);

        Circle knob = new Circle(KNOB_R);
        knob.setFill(Color.WHITE);
        knob.setTranslateX(selected ? KNOB_X : -KNOB_X);

        StackPane switchGraphic = new StackPane(track, knob);
        switchGraphic.setMinSize(SWITCH_W, SWITCH_H);
        switchGraphic.setPrefSize(SWITCH_W, SWITCH_H);
        switchGraphic.setMaxSize(SWITCH_W, SWITCH_H);

        button.setGraphic(switchGraphic);
        button.setSelected(selected);
        onChange.accept(selected);

        button.selectedProperty().addListener((obs, oldVal, newVal) -> {
            animateSwitch(track, knob, newVal);
            onChange.accept(newVal);
        });
    }

    private void animateSwitch(Rectangle track, Circle knob, boolean selected) {
        FillTransition fill = new FillTransition(SWITCH_DURATION, track, (Color) track.getFill(), selected ? SWITCH_ON : SWITCH_OFF);
        fill.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slide = new TranslateTransition(SWITCH_DURATION, knob);
        slide.setToX(selected ? KNOB_X : -KNOB_X);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(fill, slide).play();
    }
}
