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
package io.minecloud.bukkit;

import io.minecloud.MineCloud;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.binary.MessageOutputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.server.Server;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.logging.Level;

public class MineCloudPlugin extends JavaPlugin {
    private TPSTracker tracker;
    private MongoDatabase mongo;
    private RedisDatabase redis;
    private ObjectId serverId;

    @Override
    public void onEnable() {
        MineCloud.environmentSetup();

        serverId = new ObjectId(System.getenv("server_id"));
        mongo = MineCloud.instance().mongo();
        redis = MineCloud.instance().redis();
        tracker = new TPSTracker();

        tracker.runTaskTimer(this, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                Server server = server();
                Runtime runtime = Runtime.getRuntime();

                server.setRamUsage((int) ((runtime.totalMemory() - runtime.freeMemory()) / 1048576));

                synchronized (tracker) {
                    server.setTps(tracker.fetchTps());
                }

                mongo.repositoryBy(Server.class).update(server);
            }
        }.runTaskTimerAsynchronously(this, 0, 600);

        redis.addChannel(SimpleRedisChannel.create("server-create-notif", redis));
        redis.addChannel(SimpleRedisChannel.create("server-shutdown-notif", redis));

        getServer().getPluginManager().registerEvents(new PlayerTracker(), this);

        try {
            MessageOutputStream os = new MessageOutputStream();

            os.writeString(server().objectId().toString());

            redis.channelBy("server-create-notif").publish(os.toMessage());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to publish server create message, shutting down", e);
            Bukkit.shutdown();
        }
    }

    @Override
    public void onDisable() {
        mongo.repositoryBy(Server.class).remove(server());
    }

    public Server server() {
        return mongo.repositoryBy(Server.class)
                .findFirst((server) -> server.objectId().equals(serverId));
    }

    public MongoDatabase mongo() {
        return mongo;
    }
}
