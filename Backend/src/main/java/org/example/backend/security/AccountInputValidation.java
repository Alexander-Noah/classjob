package org.example.backend.security;

import org.example.backend.entity.UserEntity;

/** Shared account input bounds, enforced before persistence and password hashing. */
public final class AccountInputValidation {
    public static final int USERNAME_MAX = 100;
    public static final int PASSWORD_MAX = 128;
    private AccountInputValidation() {}

    public static void text(String label, String value, int max, boolean required) {
        if (required && (value == null || value.isBlank()))
            throw new IllegalArgumentException("请填写" + label);
        if (value == null) return;
        if (value.length() > max) throw new IllegalArgumentException(label + "不能超过 " + max + " 个字符");
        if (value.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(label + "不能包含控制字符");
    }

    public static void login(String username, String password) {
        text("账号", username, USERNAME_MAX, true);
        text("密码", password, PASSWORD_MAX, true);
        // Existing accounts may have legacy passwords; complexity is checked on creation/change.
    }

    public static void profile(UserEntity user) {
        if (user == null) throw new IllegalArgumentException("请填写账号信息");
        if (user.getPhone() != null) user.setPhone(user.getPhone().trim());
        if (user.getEmail() != null) user.setEmail(user.getEmail().trim());
        text("姓名", user.getRealName(), 100, false);
        text("部门", user.getDepartment(), 160, false);
        text("手机号", user.getPhone(), 40, false);
        text("邮箱", user.getEmail(), 255, false);
        text("头像地址", user.getAvatarUrl(), 500, false);
        String avatar = user.getAvatarUrl();
        if (avatar != null && !avatar.isBlank()) {
            if (avatar.startsWith("//") || avatar.contains("\\")) throw new IllegalArgumentException("头像地址格式不正确");
            if (avatar.matches("(?is)^[a-z][a-z0-9+.-]*:.*")) {
                try {
                    java.net.URI uri = java.net.URI.create(avatar);
                    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                            || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) { throw new IllegalArgumentException("头像地址只支持 HTTP、HTTPS 或平台图片路径"); }
            }
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()
                && !user.getPhone().matches("[+0-9() .-]{3,40}"))
            throw new IllegalArgumentException("联系电话格式不正确");
        if (user.getEmail() != null && !user.getEmail().isBlank()
                && !user.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("邮箱格式不正确");
    }

    public static void status(Integer status) {
        if (status != null && status != 0 && status != 1)
            throw new IllegalArgumentException("账号状态只能为 0 或 1");
    }

    public static void role(String role) {
        if (!java.util.Set.of("systemAdmin", "labAdmin", "academic").contains(role == null ? "" : role))
            throw new IllegalArgumentException("请选择有效且未停用的角色");
    }
}
