package com.aiimoment.account;

import com.aiimoment.Main;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 每个账户独立的设置节点：{@code com/aiimoment/profile/<accountId>}。
 * 首次为空时从旧的全局 Preferences 迁移一份，避免升级后丢设置。
 */
public final class AccountScopedPreferences {

    private static final String PROFILE_ROOT = "com/aiimoment/profile";

    private static final String KEY_FOLLOW_SYSTEM = "settings.follow_system_theme";
    private static final String KEY_AI_ASSIST = "settings.ai_assist";
    private static final String KEY_AUTO_EDIT = "settings.auto_edit";
    private static final String THEME_KEY = com.aiimoment.AppTheme.THEME_PREF_KEY;

    private AccountScopedPreferences() {
    }

    public static Preferences forAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            accountId = AccountStore.DEFAULT_ACCOUNT_ID;
        }
        String safe = accountId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return Preferences.userRoot().node(PROFILE_ROOT).node(safe);
    }

    /**
     * 若该账户节点下尚无设置，则从旧包节点复制默认项。
     */
    public static void migrateLegacyIfEmpty(String accountId) {
        Preferences profile = forAccountId(accountId);
        try {
            String[] keys = profile.keys();
            if (keys != null && keys.length > 0) {
                return;
            }
        } catch (BackingStoreException e) {
            return;
        }
        Preferences legacySettings = Preferences.userNodeForPackage(com.aiimoment.controller.SettingsPageController.class);
        profile.putBoolean(KEY_FOLLOW_SYSTEM, legacySettings.getBoolean(KEY_FOLLOW_SYSTEM, false));
        profile.putBoolean(KEY_AI_ASSIST, legacySettings.getBoolean(KEY_AI_ASSIST, true));
        profile.putBoolean(KEY_AUTO_EDIT, legacySettings.getBoolean(KEY_AUTO_EDIT, true));

        Preferences legacyMain = Preferences.userNodeForPackage(Main.class);
        String theme = legacyMain.get(THEME_KEY, null);
        if (theme != null) {
            profile.put(THEME_KEY, theme);
        }
    }
}
