package com.aiimoment.account;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 账户列表与当前账户 id 的本地持久化（~/.aiimoment/accounts.properties）。
 */
public final class AccountStore {

    public static final String DEFAULT_ACCOUNT_ID = "local_default";

    private static final AccountStore INSTANCE = new AccountStore();

    private final Path baseDir;
    private final Path accountsFile;
    private final Path avatarsDir;

    private final Map<String, AccountRecord> accounts = new LinkedHashMap<>();
    private String currentAccountId = DEFAULT_ACCOUNT_ID;

    private AccountStore() {
        String home = System.getProperty("user.home", ".");
        baseDir = Path.of(home, ".aiimoment");
        accountsFile = baseDir.resolve("accounts.properties");
        avatarsDir = baseDir.resolve("avatars");
    }

    public static AccountStore getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        accounts.clear();
        if (!Files.isRegularFile(accountsFile)) {
            seedDefaultUnpersisted();
            return;
        }
        Properties p = new Properties();
        try (InputStreamReader r = new InputStreamReader(Files.newInputStream(accountsFile), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            seedDefaultUnpersisted();
            return;
        }
        currentAccountId = p.getProperty("current", DEFAULT_ACCOUNT_ID);
        String idsRaw = p.getProperty("ids", DEFAULT_ACCOUNT_ID);
        List<String> ids = Arrays.stream(idsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            ids.add(DEFAULT_ACCOUNT_ID);
        }
        for (String id : ids) {
            String name = p.getProperty(keyName(id), defaultNameFor(id));
            String email = p.getProperty(keyEmail(id), defaultEmailFor(id));
            String avatar = p.getProperty(keyAvatar(id), "");
            if (avatar != null && avatar.isEmpty()) {
                avatar = null;
            }
            accounts.put(id, new AccountRecord(id, name, email, avatar));
        }
        if (!accounts.containsKey(currentAccountId)) {
            currentAccountId = ids.get(0);
        }
    }

    private void seedDefaultUnpersisted() {
        currentAccountId = DEFAULT_ACCOUNT_ID;
        accounts.put(DEFAULT_ACCOUNT_ID, new AccountRecord(DEFAULT_ACCOUNT_ID, "未登录用户", "未绑定邮箱", null));
    }

    private static String defaultNameFor(String id) {
        return DEFAULT_ACCOUNT_ID.equals(id) ? "未登录用户" : "用户";
    }

    private static String defaultEmailFor(String id) {
        return DEFAULT_ACCOUNT_ID.equals(id) ? "未绑定邮箱" : "";
    }

    private static String keyName(String id) {
        return "name." + id;
    }

    private static String keyEmail(String id) {
        return "email." + id;
    }

    private static String keyAvatar(String id) {
        return "avatar." + id;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(baseDir);
            Properties p = new Properties();
            p.setProperty("current", currentAccountId);
            p.setProperty("ids", String.join(",", accounts.keySet()));
            for (AccountRecord a : accounts.values()) {
                p.setProperty(keyName(a.getId()), a.getDisplayName());
                p.setProperty(keyEmail(a.getId()), a.getEmail());
                if (a.getAvatarPath() != null && !a.getAvatarPath().isEmpty()) {
                    p.setProperty(keyAvatar(a.getId()), a.getAvatarPath());
                }
            }
            try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(accountsFile), StandardCharsets.UTF_8)) {
                p.store(w, "AIiMoment accounts");
            }
        } catch (IOException ignored) {
            /* 本地持久化失败时仍允许继续使用内存状态 */
        }
    }

    public synchronized List<AccountRecord> listAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    public synchronized AccountRecord getCurrent() {
        AccountRecord r = accounts.get(currentAccountId);
        if (r == null) {
            r = new AccountRecord(currentAccountId, "未登录用户", "未绑定邮箱", null);
            accounts.put(currentAccountId, r);
        }
        return r;
    }

    public synchronized String getCurrentAccountId() {
        return currentAccountId;
    }

    public synchronized void setCurrentAccountId(String id) {
        if (!accounts.containsKey(id)) {
            return;
        }
        this.currentAccountId = id;
        save();
    }

    public synchronized void updateCurrentProfile(String displayName, String email) {
        AccountRecord r = getCurrent();
        if (displayName != null) {
            r.setDisplayName(displayName.trim());
        }
        if (email != null) {
            r.setEmail(email.trim());
        }
        save();
    }

    public synchronized void updateCurrentAvatarPath(String pathOrNull) {
        AccountRecord r = getCurrent();
        r.setAvatarPath(pathOrNull);
        save();
    }

    public synchronized AccountRecord addAccount(String displayName, String email) {
        String id = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        AccountRecord r = new AccountRecord(id,
                displayName != null && !displayName.isBlank() ? displayName.trim() : "新用户",
                email != null ? email.trim() : "",
                null);
        accounts.put(id, r);
        currentAccountId = id;
        save();
        return r;
    }

    /**
     * 将本地图片复制到应用数据目录并返回绝对路径；失败时返回 null。
     */
    public synchronized String importAvatarFile(String accountId, Path sourceFile) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(sourceFile, "sourceFile");
        String fn = sourceFile.getFileName().toString();
        String lower = fn.toLowerCase();
        String ext;
        if (lower.endsWith(".png")) {
            ext = ".png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            ext = ".jpg";
        } else if (lower.endsWith(".gif")) {
            ext = ".gif";
        } else if (lower.endsWith(".webp")) {
            ext = ".webp";
        } else {
            ext = ".img";
        }
        try {
            Files.createDirectories(avatarsDir);
            Path dest = avatarsDir.resolve(accountId + ext);
            Files.copy(sourceFile, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (IOException e) {
            return null;
        }
    }

    public Path getAvatarsDirectory() {
        return avatarsDir;
    }

    /** 首次安装尚无 accounts 文件时，把内存中的默认账户写入磁盘。 */
    public synchronized void persistIfNew() {
        try {
            if (!Files.isRegularFile(accountsFile)) {
                save();
            }
        } catch (Exception ignored) {
            /* ignore */
        }
    }
}
