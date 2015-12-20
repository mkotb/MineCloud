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
package io.minecloud.daemon;

import io.minecloud.MineCloud;
import io.minecloud.db.Credentials;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.MessageType;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.NodeRepository;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerMetadata;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import org.mongodb.morphia.query.Query;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MineCloudDaemon {
    private static MineCloudDaemon instance;

    private final String node;
    private final RedisDatabase redis;
    private final MongoDatabase mongo;

    private MineCloudDaemon(Properties properties) {
        redis = MineCloud.instance().redis();
        mongo = MineCloud.instance().mongo();
        node = (String) properties.get("node-name");

        instance = this;

        redis.addChannel(SimpleRedisChannel.create("server-create", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    if (!stream.readString().equalsIgnoreCase(node)) {
                        return;
                    }

                    Network network = mongo.repositoryBy(Network.class).findFirst(stream.readString());
                    ServerType type = mongo.repositoryBy(ServerType.class).findFirst(stream.readString());
                    List<ServerMetadata> metadata = new ArrayList<>();
                    int size = stream.readVarInt32();

                    for (int i = 0; i < size; i++) {
                        metadata.add(new ServerMetadata(stream.readString(), stream.readString()));
                    }

                    Deployer.deployServer(network, type, metadata);
                }));

        redis.addChannel(SimpleRedisChannel.create("server-kill", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    if (!stream.readString().equalsIgnoreCase(node))
                        return;

                    Server server = mongo.repositoryBy(Server.class).findFirst(stream.readString());

                    if (!server.node().name().equals(node)) {
                        MineCloud.logger().log(Level.SEVERE, "Invalid request was sent to kill a server " +
                                "not on the current node");
                        return;
                    }

                    try {
                        new ProcessBuilder().command("/usr/bin/kill", "-9", String.valueOf(Deployer.pidOf(server.name()))).start();
                        MineCloud.logger().info("Killed server " + server.name()
                                + " with container id " + server.containerId());
                        mongo.repositoryBy(Server.class).delete(server);
                        try (Jedis jedis = this.redis.grabResource()) {
                            jedis.hdel("server:" + server.entityId(), "heartbeat");
                        }
                    } catch (IOException e) {
                        MineCloud.logger().log(Level.SEVERE, "Was unable to kill a server", e);
                    }
                }));

        redis.addChannel(SimpleRedisChannel.create("bungee-create", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    if (!stream.readString().equalsIgnoreCase(node))
                        return;

                    Network network = mongo.repositoryBy(Network.class).findFirst(stream.readString());
                    BungeeType type = mongo.repositoryBy(BungeeType.class).findFirst(stream.readString());

                    Deployer.deployBungee(network, type);
                }));

        redis.addChannel(SimpleRedisChannel.create("bungee-kill", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    if (!stream.readString().equalsIgnoreCase(node))
                        return;

                    Bungee bungee = mongo.repositoryBy(Bungee.class).findFirst(stream.readString());

                    if (!bungee.node().name().equals(node)) {
                        MineCloud.logger().log(Level.SEVERE, "Invalid request was sent to kill a bungee " +
                                "not on the current node");
                        return;
                    }

                    try {
                        new ProcessBuilder().command("/usr/bin/kill", "-9", String.valueOf(Deployer.pidOf("bungee"))).start();
                        MineCloud.logger().info("Killed bungee " + bungee.name()
                                + " with container id " + bungee.containerId());

                        mongo.repositoryBy(Bungee.class).delete(bungee);
                    } catch (IOException e) {
                        MineCloud.logger().log(Level.SEVERE, "Was unable to kill a server", e);
                    }
                }));

        redis.addChannel(SimpleRedisChannel.create("server-start-notif", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY)
                        return;

                    MessageInputStream stream = message.contents();
                    Server server = mongo.repositoryBy(Server.class).findFirst(stream.readString());

                    if (!server.node().name().equals(node))
                        return;

                    server.setContainerId(String.valueOf(Deployer.pidOf(server.name())));
                    mongo.repositoryBy(Server.class).save(server);
                }));
        redis.addChannel(SimpleRedisChannel.create("server-shutdown-notif", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY)
                        return;

                    MessageInputStream stream = message.contents();
                    File file = new File("/var/minecloud/" + stream.readString());

                    if (file.exists()) {
                        file.delete();
                    }
                }));

        new StatisticsWatcher().start();

        while (!Thread.currentThread().isInterrupted()) {
            this.redis.connected(); //Checks for Redis death, if it's dead it will reconnect.

            BungeeRepository bungeeRepo = mongo.repositoryBy(Bungee.class);
            ServerRepository repository = mongo.repositoryBy(Server.class);
            Node node = node();
            Query<Server> query = repository.createQuery()
                    .field("node").equal(node)
                    .field("tps").notEqual(-1);
            List<Server> nodeServers = repository.find(query).asList();
            List<String> names = nodeServers.stream()
                    .map(Server::name)
                    .collect(Collectors.toList());

            List<File> files = files(new File("/var/minecloud"));

            files.stream().filter(file -> !file.getName().equalsIgnoreCase("bungee")).forEach(file1 -> {
                String name = file1.getName();

                if (!names.contains(name)) {
                    MineCloud.logger().info("Found directory for server not in the DB, " + name);

                    if (file1.exists()) {
                        file1.delete();
                    }
                }
            });

            nodeServers.forEach((server) -> {
                File runDir = new File("/var/minecloud/" + server.name());

                if (!runDir.exists()) {
                    repository.delete(server);
                    MineCloud.logger().info("Removed " + server.containerId() + " from DB due to not existing on node");
                    return;
                }

                try {
                    if ((System.currentTimeMillis() - Deployer.timeStarted(server.name())) < 600_000L) {
                        return;
                    }

                    if (Deployer.isRunning(server.name())) {
                        return;
                    }

                    repository.delete(server);
                    names.remove(server.name());
                    try (Jedis jedis = this.redis.grabResource()) {
                        jedis.hdel("server:" + server.entityId(), "heartbeat");
                    }
                    MineCloud.logger().info("Removed dead server (" + server.name() + ")");
                } catch (IOException | InterruptedException ex) {
                    if (!(ex instanceof NoSuchFileException)) {
                        MineCloud.logger().log(Level.SEVERE, "Was unable to check if server is running", ex);
                    }
                }
            });

            try (Jedis jedis = this.redis.grabResource()) {
                nodeServers.forEach(server ->  {
                    Map<String, String> hResult = jedis.hgetAll("server:" + server.entityId());

                    if (hResult == null || hResult.isEmpty()) {
                        return; //Prevent loop from dying.
                    }

                    long heartbeat = Long.valueOf(hResult.get("heartbeat"));
                    long difference = System.currentTimeMillis() - heartbeat;
                    if (difference > 30000L) {
                        try {
                            new ProcessBuilder().command("/usr/bin/kill", "-9", String.valueOf(Deployer.pidOf(server.name()))).start(); //Murder server in cold blood.
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        jedis.hdel("server:" + server.entityId(), "heartbeat");
                        repository.delete(server);
                        names.remove(server.name());
                        File runDir = new File("/var/minecloud/" + server.name());

                        if (runDir.exists()) {
                            runDir.delete();
                        }

                        MineCloud.logger().log(Level.WARNING, "Found server not updated in 30s, killing (" + server.name() + ")");
                    }
                });
            }

            if (bungeeRepo.findOne("_id", node.publicIp()) != null) {
                try {
                    if ((System.currentTimeMillis() - Deployer.timeStarted("bungee")) > 600_000L & !Deployer.isRunning("bungee")) {
                        bungeeRepo.deleteById(node.publicIp());
                        MineCloud.logger().info("Removed dead bungee (" + node.publicIp() + ")");
                    }
                } catch (IOException | InterruptedException ex) {
                    if (!(ex instanceof NoSuchFileException)) {
                        MineCloud.logger().log(Level.SEVERE, "Was unable to check if bungee is running", ex);
                    }
                }
            }

            File appContainer = new File("/var/minecloud");

            names.add("bungee"); // don't remove bungee servers

            if (!appContainer.isDirectory()) {
                appContainer.delete();
            }

            if (!appContainer.exists()) {
                appContainer.mkdirs();
            }

            for (File f : appContainer.listFiles(File::isDirectory)) {
                if (!names.contains(f.getName())) {
                    try {
                        Runtime.getRuntime().exec(("/usr/bin/rm -rf " + f.getAbsolutePath()).split(" "));
                        MineCloud.logger().info("Deleted folder of dead server " + f.getName());
                    } catch (IOException ignored) {
                    }
                }
            }

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        File configFolder = new File("/etc/minecloud/");
        File file = new File(configFolder, "details.properties");

        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        properties.load(new FileInputStream(file));

        if (!properties.containsKey("mongo-hosts")) {
            MineCloud.runSetup(properties, file);
            new MineCloudDaemon(properties);

            properties = null;
            return;
        }

        Credentials mongo = new Credentials(properties.getProperty("mongo-hosts").split(";"),
                properties.getProperty("mongo-username"),
                properties.getProperty("mongo-password").toCharArray(),
                properties.getProperty("mongo-database"));
        Credentials redis = new Credentials(new String[] {properties.getProperty("redis-host")},
                "",
                properties.getProperty("redis-password").toCharArray());

        MineCloud.instance().initiateMongo(mongo);
        MineCloud.instance().initiateRedis(redis);

        new MineCloudDaemon(properties);
    }

    public static MineCloudDaemon instance() {
        return instance;
    }

    public Node node() {
        return ((NodeRepository) mongo.repositoryBy(Node.class)).nodeBy(node);
    }

    private List<File> files(File directory) {
        List<File> files = new ArrayList<>();
        File[] dirFiles = directory.listFiles();
        files.addAll(Arrays.asList(dirFiles));

        for (File file : dirFiles) {
            if (file.isDirectory()) {
                files.addAll(files(file.getAbsoluteFile()));
            }
        }

        return files;
    }
}
