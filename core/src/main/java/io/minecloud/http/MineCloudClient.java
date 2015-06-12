/*
 * Copyright (c) 2015, Mazen Kotb <email@mazenmc.io>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package io.minecloud.http;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.http.msg.AuthenticateMessage;
import io.minecloud.http.msg.Message;
import io.minecloud.http.msg.Response;
import io.minecloud.http.msg.ResponseCallback;

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
        sendMessage(new AuthenticateMessage(user), (r) -> {});
    }

    public void sendMessage(Message message, ResponseCallback callback) {
        Unirest.post(hostname + ":2572" + message.path())
                .body(MineCloud.fetchGson().toJson(message))
                .asStringAsync(new Callback<String>() {
                    @Override
                    public void completed(HttpResponse<String> httpResponse) {
                        Response response = MineCloud.fetchGson().fromJson(httpResponse.getBody(),
                                Response.class);
                        callback.callback(response);
                    }

                    @Override
                    public void failed(UnirestException e) {
                        throw new MineCloudException("Failed to send POST request to " + hostname, e);
                    }

                    @Override
                    public void cancelled() {
                        throw new MineCloudException("POST request was oddly caused... what happened here?");
                    }
                });
    }


}
