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

import com.google.common.io.Files;
import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.MessageType;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.plugins.PluginType;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.type.ServerType;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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

        BungeeType type = bungee().type();

        File nContainer = new File("nplugins/");
        nContainer.mkdirs();

        type.plugins().forEach((plugin) -> {
            String version = plugin.version();
            PluginType pluginType = plugin.type();
            File pluginsContainer = new File("/mnt/minecloud/plugins/",
                    pluginType.name() + "/" + version);
            List<File> plugins = new ArrayList<>();

            getLogger().info("Loading " + pluginType.name() + "...");

            if (validateFolder(pluginsContainer, pluginType, version))
                return;

            for (File f : pluginsContainer.listFiles()) {
                if (f.isDirectory())
                    continue; // ignore directories
                File pl = new File(nContainer, f.getName());

                try {
                    Files.copy(f, pl);
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, "Could not load " + pluginType.name() +
                            ", printing stacktrace...");
                    ex.printStackTrace();
                    return;
                }

                plugins.add(pl);
            }

            File configs = new File("/mnt/minecloud/configs/",
                    pluginType.name() + "/" + version);
            File configContainer = new File(nContainer, pluginType.name());

            if (validateFolder(configs, pluginType, version))
                return;

            copyFolder(configs, configContainer);
        });

        // release plugin manager lock
        try {
            Field f = PluginManager.class.getDeclaredField("toLoad");

            f.setAccessible(true);
            f.set(getProxy().getPluginManager(), new HashMap<>());
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        getProxy().getPluginManager().detectPlugins(nContainer);
        getProxy().getPluginManager().loadPlugins();
        getProxy().getPluginManager().getPlugins().stream()
                .filter((p) -> !p.getDescription().getName().equals("MineCloud-Bungee"))
                .forEach(Plugin::onEnable);
    }

    private boolean validateFolder(File file, PluginType pluginType, String version) {
        if (!file.exists()) {
            getLogger().info(file.getPath() + " does not exist! Cannot load " + pluginType.name());
            return true;
        }

        if (!(file.isDirectory()) || file.listFiles() == null
                || file.listFiles().length < 1) {
            getLogger().info(pluginType.name() + " " + version +
                    " has either no files or has an invalid setup");
            return true;
        }

        return false;
    }

    private void copyFolder(File folder, File folderContainer) {
        folderContainer.mkdirs();

        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                File newContainer = new File(folderContainer, f.getName());
                copyFolder(f, newContainer);
            }

            try {
                Files.copy(f, new File(folderContainer, f.getName()));
            } catch (IOException ex) {
                throw new MineCloudException(ex);
            }
        }
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
                .findFirst((bungee) -> bungee.entityId().equals(System.getenv("bungee_id")));
    }
}
