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

import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.Message;
import redis.clients.jedis.Jedis;

import java.nio.charset.Charset;

public abstract class RedisChannel {
    protected final RedisDatabase database;
    protected final String channel;

    protected RedisChannel(RedisDatabase database, String channel) {
        this.database = database;
        this.channel = channel;
        Jedis resource = database.grabResource();

        resource.subscribe(ChannelPubSub.create(this), channel.getBytes(Charset.forName("UTF-8")));
        database.returnResource(resource);
    }

    public void publish(Message message) {
        Jedis resource = database.grabResource();

        resource.publish(channel.getBytes(Charset.forName("UTF-8")), message.raw());
        database.returnResource(resource);
    }

    public abstract void handle(Message message);
}
