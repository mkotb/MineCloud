package io.minecloud.daemon;

import io.minecloud.MineCloud;
import io.minecloud.db.Credentials;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.MessageType;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.NodeRepository;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerMetadata;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import org.mongodb.morphia.query.Query;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

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
                        new ProcessBuilder().command("/usr/bin/kill -9 " + Deployer.pidOf(server.name())).start();
                        MineCloud.logger().info("Killed server " + server.name()
                                + " with container id " + server.containerId());
                        mongo.repositoryBy(Server.class).delete(server);
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
                        new ProcessBuilder().command("/usr/bin/kill -9 " + Deployer.pidOf("bungee")).start();
                        MineCloud.logger().info("Killed bungee " + bungee.name()
                                + " with container id " + bungee.containerId());

                        mongo.repositoryBy(Bungee.class).delete(bungee);
                    } catch (IOException e) {
                        MineCloud.logger().log(Level.SEVERE, "Was unable to kill a server", e);
                    }
                }));

        redis.addChannel(SimpleRedisChannel.create("server-shutdown-notif", redis));

        new StatisticsWatcher().start();

        while (!Thread.currentThread().isInterrupted()) {
            ServerRepository repository = mongo.repositoryBy(Server.class);
            Query<Server> query = repository.createQuery()
                    .field("node").equal(node())
                    .field("port").notEqual(-1)
                    .field("tps").notEqual(-1);

            repository.find(query).asList().forEach((server) -> {
                File runDir = new File("/var/run/" + server.name());

                if (!runDir.exists()) {
                    repository.delete(server);
                    MineCloud.logger().info("Removed " + server.containerId() + " from DB due to not existing on node");
                    return;
                }

                try {
                    if (Deployer.isRunning(server.name())) {
                        return;
                    }

                    repository.delete(server);
                    MineCloud.logger().info("Removed dead server (" + server.name() + ")");
                } catch (IOException | InterruptedException ex) {
                    MineCloud.logger().log(Level.SEVERE, "Was unable to check if server is running", ex);
                }
            });

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
}
