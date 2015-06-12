package io.minecloud.http;

import io.minecloud.MineCloudException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MineCloudClient {
    private final String hostname;
    private User user;

    private MineCloudClient(String controller) {
        this.hostname = controller;
    }

    public static MineCloudClient create(String controller) {
        return new MineCloudClient(controller);
    }

    public String hostname() {
        return hostname;
    }

    public void initiate(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            password = new String(digest.digest(password.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new MineCloudException("Unable to hash password in SHA256", e);
        }

        if (user != null)
            throw new UnsupportedOperationException("Cannot reinstantiate a client!");

        user = User.create(username, password);

    }
}
