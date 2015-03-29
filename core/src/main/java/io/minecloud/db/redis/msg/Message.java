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
package io.minecloud.db.redis.msg;

import io.minecloud.annotation.Immutable;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import org.json.JSONObject;

import java.nio.charset.Charset;

@Immutable
public final class Message {
    private final MessageType type;
    private final byte[] contents;

    private Message(byte[] contents) {
        this.contents = contents;
        this.type = MessageType.BINARY;
    }

    private Message(JSONObject object) {
        this.type = MessageType.JSON;
        this.contents = object.toString().getBytes(Charset.forName("UTF-8"));
    }

    private Message(String content) {
        this.type = MessageType.STRING;
        this.contents = content.getBytes(Charset.forName("UTF-8"));
    }

    public static Message messageFrom(byte[] contents) {
        return new Message(contents);
    }

    public static Message messageFrom(JSONObject object) {
        return new Message(object);
    }

    public static Message messageFrom(String content) {
        return new Message(content);
    }

    public MessageInputStream contents() {
        return new MessageInputStream(contents);
    }

    public byte[] raw() {
        byte[] raw = new byte[contents.length + 1];

        raw[0] = (byte) type.ordinal();
        System.arraycopy(contents, 0, raw, 1, contents.length);

        return raw;
    }

    public MessageType type() {
        return type;
    }

    public JSONObject asJson() {
        if (type() != MessageType.JSON)
            throw new UnsupportedOperationException("Cannot convert " + type() + " to JSON");

        return new JSONObject(toString());
    }

    @Override
    public String toString() {
        if (type() == MessageType.BINARY)
            throw new UnsupportedOperationException("Cannot convert non-string binary to String");

        return new String(contents, Charset.forName("UTF-8"));
    }
}
