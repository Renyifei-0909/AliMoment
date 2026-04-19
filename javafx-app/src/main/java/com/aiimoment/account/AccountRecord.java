package com.aiimoment.account;

import java.util.Objects;

/**
 * 本地持久化的一条账户信息（非安全凭证存储，仅展示与切换用）。
 */
public final class AccountRecord {

    private final String id;
    private String displayName;
    private String email;
    /** 本地头像文件绝对路径，可为空 */
    private String avatarPath;

    public AccountRecord(String id, String displayName, String email, String avatarPath) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName != null ? displayName : "";
        this.email = email != null ? email : "";
        this.avatarPath = avatarPath;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public AccountRecord copy() {
        return new AccountRecord(id, displayName, email, avatarPath);
    }
}
