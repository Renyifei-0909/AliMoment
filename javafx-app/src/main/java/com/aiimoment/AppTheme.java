package com.aiimoment;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.prefs.Preferences;

/**
 * 应用级浅色 / 深色主题：通过场景根 {@link StackPane} 上的 {@code theme-light} 样式类切换，
 * 具体配色由 {@code /styles/theme-light.css} 覆盖默认的 {@code app.css}。
 */
public final class AppTheme {

    /** 与 {@link Main} 及各账户 profile 节点中保存的主题键一致 */
    public static final String THEME_PREF_KEY = "ui.theme";
    private static final String PREF_KEY = THEME_PREF_KEY;
    private static final String VALUE_DARK = "dark";
    private static final String VALUE_LIGHT = "light";
    private static final String STYLE_LIGHT = "theme-light";

    private static final Preferences PREF = Preferences.userNodeForPackage(Main.class);

    private AppTheme() {
    }

    public static boolean isLight() {
        return VALUE_LIGHT.equalsIgnoreCase(PREF.get(PREF_KEY, VALUE_DARK));
    }

    /** 从任意 Preferences 节点读取是否浅色（缺省为深色）。 */
    public static boolean readLight(Preferences node) {
        if (node == null) {
            return isLight();
        }
        return VALUE_LIGHT.equalsIgnoreCase(node.get(PREF_KEY, VALUE_DARK));
    }

    public static void applySaved(StackPane shell, Scene scene) {
        apply(shell, scene, isLight());
    }

    /**
     * @param light true 为浅色主题，false 为深色（默认）
     */
    public static void apply(StackPane shell, Scene scene, boolean light) {
        apply(shell, scene, light, PREF);
    }

    /**
     * 切换场景样式，并将主题值写入给定的一个或多个 Preferences 节点（可传 null 跳过某项）。
     */
    public static void apply(StackPane shell, Scene scene, boolean light, Preferences primaryPersist, Preferences... extraPersist) {
        if (shell == null || scene == null) {
            return;
        }
        if (light) {
            if (!shell.getStyleClass().contains(STYLE_LIGHT)) {
                shell.getStyleClass().add(STYLE_LIGHT);
            }
            scene.setFill(Color.web("#f0f0f0"));
        } else {
            shell.getStyleClass().remove(STYLE_LIGHT);
            scene.setFill(Color.web("#121212"));
        }
        if (primaryPersist != null) {
            primaryPersist.put(PREF_KEY, light ? VALUE_LIGHT : VALUE_DARK);
        }
        if (extraPersist != null) {
            for (Preferences p : extraPersist) {
                if (p != null) {
                    p.put(PREF_KEY, light ? VALUE_LIGHT : VALUE_DARK);
                }
            }
        }
    }
}
