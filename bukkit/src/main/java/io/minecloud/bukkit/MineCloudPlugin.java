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
import io.minecloud.MineCloudException;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.binary.MessageOutputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.plugins.PluginType;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.World;
import io.minecloud.models.server.type.ServerType;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MineCloudPlugin extends JavaPlugin {
    private TPSTracker tracker;
    private MongoDatabase mongo;
    private RedisDatabase redis;
    private String serverId;

    @Override
    public void onEnable() {
        MineCloud.environmentSetup();

        serverId = System.getenv("server_id");
        mongo = MineCloud.instance().mongo();
        redis = MineCloud.instance().redis();
        tracker = new TPSTracker();

        tracker.runTaskTimer(this, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                Server server = server();

                if (server == null) {
                    getLogger().log(Level.INFO, "Server removed from database, going down...");
                    getServer().shutdown();
                    return;
                }

                Runtime runtime = Runtime.getRuntime();

                server.setRamUsage((int) ((runtime.totalMemory() - runtime.freeMemory()) / 1048576));

                synchronized (tracker) {
                    server.setTps(tracker.fetchTps());
                }

                mongo.repositoryBy(Server.class).save(server);
            }
        }.runTaskTimerAsynchronously(this, 0, 200);

        redis.addChannel(SimpleRedisChannel.create("server-start-notif", redis));
        redis.addChannel(SimpleRedisChannel.create("server-shutdown-notif", redis));

        getServer().getPluginManager().registerEvents(new PlayerTracker(), this);

        // start loading plugins and additional worlds

        ServerType type = server().type();

        type.worlds().forEach((world) -> {
            File worldFolder = new File("/mnt/minecloud/worlds/",
                    world.name() + "/" + world.version());

            if (validateFolder(worldFolder, world)) {
                return;
            }

            File wrld = new File(world.name());

            wrld.mkdirs();
            copyFolder(worldFolder, wrld);

            Bukkit.createWorld(new WorldCreator(world.name()));
        });

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
                File pl = new File("plugins/" + f.getName());

                FileUtil.copy(f, pl);
                plugins.add(pl);
            }
            
            File configs = new File("/mnt/minecloud/configs/", 
                    pluginType.name() + "/" + version);
            File configContainer = new File("plugins/" + pluginType.name());
            
            if (validateFolder(configs, pluginType, version))
                return;

            copyFolder(configs, configContainer);

            plugins.forEach((f) -> {
                try {
                    Bukkit.getPluginManager().loadPlugin(f);
                } catch (Exception ex) {
                    new MineCloudException("Could not load " + pluginType.name()
                            + " due to an unexpected exception", ex);
                }
            });
        });

        try {
            MessageOutputStream os = new MessageOutputStream();

            os.writeString(server().entityId());

            redis.channelBy("server-start-notif").publish(os.toMessage());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to publish server create message, shutting down", e);
            Bukkit.shutdown();
        }
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

    private boolean validateFolder(File file, World world) {
        if (!file.exists()) {
            getLogger().info(file.getPath() + " does not exist! Cannot load " + world.name());
            return true;
        }

        if (!(file.isDirectory()) || file.listFiles() == null
                || file.listFiles().length < 1) {
            getLogger().info(world.name() + " " +  world.version() + " has either no files or has an invalid setup");
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

            FileUtil.copy(f, new File(folderContainer, f.getName()));
        }
    }

    @Override
    public void onDisable() {
        Server server = server();

        if (server != null)
            mongo.repositoryBy(Server.class).delete(server);

        try {
            MessageOutputStream os = new MessageOutputStream();

            os.writeString(serverId);

            redis.channelBy("server-shutdown-notif").publish(os.toMessage());
        } catch (IOException ex) {
            ex.printStackTrace(); // almost impossible to happen
        }
    }

    public Server server() {
        return mongo.repositoryBy(Server.class)
                .findFirst((server) -> server.entityId().equals(serverId));
    }

    public MongoDatabase mongo() {
        return mongo;
    }
}
