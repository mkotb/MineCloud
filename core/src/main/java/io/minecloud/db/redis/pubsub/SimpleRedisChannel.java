package io.minecloud.db.redis.pubsub;

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
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }
}
