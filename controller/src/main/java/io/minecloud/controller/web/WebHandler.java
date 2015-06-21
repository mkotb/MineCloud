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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.minecloud.MineCloud;
import io.minecloud.controller.user.UserManager;
import io.minecloud.controller.web.respond.Responder;
import io.minecloud.controller.web.respond.ResponseUtils;
import io.minecloud.http.User;
import io.minecloud.http.msg.AuthenticateMessage;
import io.minecloud.http.msg.Message;
import io.minecloud.http.msg.Response;
import io.minecloud.http.msg.ResponseStatus;
import spark.Spark;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class WebHandler {
    private static final Gson GSON = MineCloud.fetchGson();
    private static final WebHandler INSTANCE = new WebHandler();
    private Set<SessionToken> tokens = new HashSet<>();

    private WebHandler() {
        Spark.port(2572);
    }

    public static WebHandler instance() {
        return INSTANCE;
    }

    public void registerToken(SessionToken token) {
        tokens.add(token);
    }

    public void cleanTokens() {
        tokens.removeIf((t) -> !t.isValid());
    }

    public boolean validToken(String token) {
        cleanTokens();

        return tokens.stream()
                .filter((s) -> s.token().equals(token))
                .findFirst()
                .isPresent();
    }

    public SessionToken tokenBy(String token) {
        Optional<SessionToken> optional = tokens.stream()
                .filter((t) -> t.token().equalsIgnoreCase(token))
                .findFirst();

        return optional.isPresent() ? optional.get() : null;
    }

    public <T extends Message> void registerPost(String path, Class<T> cls, Responder<T> responder) {
        Spark.post(path, (request, response) -> {
            T message;

            try {
                message = GSON.fromJson(request.body(), cls);
            } catch (JsonSyntaxException ex) {
                response.status(400);
                return GSON.toJson(ResponseUtils.errorResponse("Invalid json body for this message!"));
            }

            if (cls != AuthenticateMessage.class && !validToken(message.token())) {
                response.status(401);
                JsonObject payload = new JsonObject();

                payload.addProperty("reason", cls.getSimpleName() + " cannot be sent without authentication!");

                return GSON.toJson(new Response(ResponseStatus.UNAUTHORIZED, payload));
            }

            Optional<User> optional = UserManager.instance().userBy(tokenBy(message.token()));
            MessageContext context = new MessageContext(optional.get(), request);

            return GSON.toJson(responder.respond(context, message));
        });
    }
}
