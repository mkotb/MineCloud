package io.minecloud.http;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class User {
    private String name;
    private String passwordHash;
    private String lastKnownAddress;
    private long lastLogin;

    private User(String name, String password) {
        this.name = name;
        this.passwordHash = password;
    }

    User() {
    }

    public static User create(String name, String passwordHash) {
        return new User(name, passwordHash);
    }

    public String name() {
        return name;
    }

    public String password() {
        return passwordHash;
    }

    public String lastKnownAddress() {
        return lastKnownAddress;
    }

    public long lastLogin() {
        return lastLogin;
    }
}
