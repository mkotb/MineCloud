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
package io.minecloud.db.redis.pubsub;

import io.minecloud.MineCloudException;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.Message;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SimpleRedisChannel extends RedisChannel {
    private List<ChannelCallback> callbacks = new LinkedList<>();

    private SimpleRedisChannel(String channel, RedisDatabase database) {
        super(channel, database);
    }

    public static SimpleRedisChannel create(String channel, RedisDatabase database) {
        return new SimpleRedisChannel(channel, database);
    }

    public SimpleRedisChannel addCallback(ChannelCallback callback) {
        callbacks.add(callback);
        return this;
    }

    public void removeCallback(int index) {
        callbacks.remove(index);
    }

    public void removeCallback(ChannelCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    public void handle(Message message) {
        callbacks.forEach((c) -> {
            try {
                c.call(message);
            } catch (Exception ex) {
                new MineCloudException(ex).printStackTrace();
            }
        });
    }
}
