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

import io.minecloud.MineCloud;
import io.minecloud.db.redis.msg.Message;
import io.minecloud.db.redis.msg.MessageType;
import org.apache.logging.log4j.Level;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedisPubSub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public final class ChannelPubSub extends BinaryJedisPubSub {
    private final RedisChannel channel;

    private ChannelPubSub(RedisChannel channel) {
        this.channel = channel;
    }

    static ChannelPubSub create(RedisChannel channel) {
        return new ChannelPubSub(channel);
    }

    @Override
    public void onMessage(byte[] c, byte[] message) {
        InputStream stream = new ByteArrayInputStream(message);
        MessageType type = MessageType.BINARY;

        try {
            type = MessageType.values()[stream.read()];
            byte[] data = new byte[message.length - 1];

            stream.read(data);

            switch (type) {
                case BINARY:
                    channel.handle(Message.messageFrom(data));
                    break;

                case JSON:
                    channel.handle(Message.messageFrom(new JSONObject(new String(data, Charset.forName("UTF-8")))));
                    break;

                case STRING:
                    channel.handle(Message.messageFrom(new String(data, Charset.forName("UTF-8"))));
                    break;
            }
        } catch (IOException ex) {
            MineCloud.logger().log(Level.ERROR,
                    "Encountered an IOException while reading a " + type.name() + " message",
                    ex);
        }
    }

    @Override
    public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        onMessage(channel, message); // receive own messages C:
    }
}
