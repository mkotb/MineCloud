package io.minecloud.db.redis.pubsub;

import io.minecloud.db.redis.msg.Message;

import java.io.IOException;

public interface ChannelCallback {

    public void call(Message message) throws IOException ;
}
