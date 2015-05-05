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
package io.minecloud.bungee;

import io.minecloud.MineCloud;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.MessageType;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.type.ServerType;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.bson.types.ObjectId;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class MineCloudPlugin extends Plugin {
    MongoDatabase mongo;
    RedisDatabase redis;

    @Override
    public void onEnable() {
        MineCloud.environmentSetup();

        mongo = MineCloud.instance().mongo();
        redis = MineCloud.instance().redis();

        redis.addChannel(SimpleRedisChannel.create("server-create-notif", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    Server server = mongo.repositoryBy(Server.class).findFirst(stream.readString());

                    addServer(server);
                }));

        redis.addChannel(SimpleRedisChannel.create("server-shutdown-notif", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    removeServer(stream.readString());
                }));

        getProxy().getScheduler().schedule(this, () -> {
            ServerType def = mongo.repositoryBy(ServerType.class).findFirst(ServerType::defaultServer);

            mongo.repositoryBy(Server.class).models().stream()
                    .filter((server) -> server.type().equals(def) && server.ramUsage() != -1)
                    .forEach(this::addServer);

            getProxy().setReconnectHandler(new ReconnectHandler(this));
            getProxy().getPluginManager().registerListener(this, new MineCloudListener(this));
        }, 0, TimeUnit.SECONDS);
    }

    public void addServer(Server server) {
        ServerInfo info = getProxy().constructServerInfo(server.name(),
                new InetSocketAddress(server.node().privateIp(), server.port()),
                "", false);

        getProxy().getServers().put(server.name(), info);
    }

    public void removeServer(String server) {
        getProxy().getServers().remove(server);
    }

    public Bungee bungee() {
        return mongo.repositoryBy(Bungee.class)
                .findFirst((bungee) -> bungee.containerId().equals(System.getenv("bungee_id")));
    }
}
