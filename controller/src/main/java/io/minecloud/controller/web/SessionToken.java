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
package io.minecloud.controller.web;

import io.minecloud.annotation.Immutable;
import io.minecloud.http.User;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Immutable
@EqualsAndHashCode
public final class SessionToken {
    private final long timeStamp;
    private final String token;
    private final User user;

    SessionToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString().replace("-", "");
        this.timeStamp = System.currentTimeMillis();

        user.setToken(token);
    }

    public static SessionToken create(User user) {
        return new SessionToken(user);
    }

    public long timeStamp() {
        return timeStamp;
    }

    public String token() {
        return token;
    }

    public User user() {
        return user;
    }

    public boolean isValid() {
        return (System.currentTimeMillis() - timeStamp) < 10_800_000;
    }
}
