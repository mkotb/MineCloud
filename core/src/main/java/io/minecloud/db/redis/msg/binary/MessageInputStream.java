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
package io.minecloud.db.redis.msg.binary;

import io.minecloud.db.redis.msg.Message;
import io.minecloud.db.redis.msg.MessageType;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class MessageInputStream extends ByteArrayInputStream {

    public MessageInputStream(byte[] buf) {
        super(buf);
    }

    public Message readMessage() {
        byte[] message = new byte[readVarInt32()];
        MessageType type = MessageType.values()[read()];
        byte[] data = new byte[message.length - 1];

        System.arraycopy(message, 1, data, 0, message.length - 1);

        switch (type) {
            case BINARY:
                return Message.messageFrom(data);

            case JSON:
                return Message.messageFrom(new JSONObject((new String(data, Charset.forName("UTF-8")))));

            case STRING:
                return Message.messageFrom(new String(data, Charset.forName("UTF-8")));

            default:
                throw new IllegalStateException("Invalid message type!");
        }
    }

    public int readVarInt32() {
        int result = 0;

        int i = 0;
        int b = read();

        while ((b & 0x80) == 0x80) {
            if (i > 21)
                throw new IllegalArgumentException("Too many bytes for VarInt32!");

            result += (b & 0x7F) << i;
            i += 7;

            b = read();
        }

        return result + ((b & 0x7F) << i);
    }

    public String readString() throws IOException {
        int length = readVarInt32();
        byte[] bytes = new byte[length];

        read(bytes);

        return new String(bytes, Charset.forName("UTF-8"));
    }

    public JSONObject readJson() throws IOException {
        return new JSONObject(readString());
    }
}
