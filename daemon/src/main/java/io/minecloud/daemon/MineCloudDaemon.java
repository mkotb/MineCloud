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

import com.mongodb.BasicDBObject;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import io.minecloud.MineCloud;
import io.minecloud.db.Credentials;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.MessageType;
import io.minecloud.db.redis.msg.binary.MessageInputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.NodeRepository;
import io.minecloud.models.server.type.ServerType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import static io.minecloud.MineCloud.logger;

public class MineCloudDaemon {
    private static MineCloudDaemon instance;

    private final String node;
    private final DockerClient dockerClient;
    private final RedisDatabase redis;
    private final MongoDatabase mongo;

    private MineCloudDaemon(Properties properties) {
        redis = MineCloud.instance().redis();
        mongo = MineCloud.instance().mongo();
        dockerClient = new DefaultDockerClient("unix:///var/run/docker.sock");

        node = (String) properties.get("node-name");
        instance = this;

        redis.addChannel(SimpleRedisChannel.create("server-create", redis)
                .addCallback((message) -> {
                    if (message.type() != MessageType.BINARY) {
                        return;
                    }

                    MessageInputStream stream = message.contents();

                    if (!stream.readString().equalsIgnoreCase(node))
                        return;

                    Network network = mongo.repositoryBy(Network.class)
                            .findFirst(new BasicDBObject("name", stream.readString()));
                    ServerType type = mongo.repositoryBy(ServerType.class)
                            .findFirst(new BasicDBObject("name", stream.readString()));

                    Deployer.deployServer(network, type);
                }));

        new StatisticsWatcher().start();
    }

    public Node node() {
        return ((NodeRepository) mongo.repositoryBy(Node.class)).nodeBy(node);
    }

    public DockerClient dockerClient() {
        return dockerClient;
    }

    public static MineCloudDaemon instance() {
        return instance;
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        File configFolder = new File("/etc/minecloud/");
        File file = new File(configFolder, "daemon/details.properties");

        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        properties.load(new FileInputStream(file));

        if (!properties.containsKey("mongo-hosts")) {
            runSetup(properties, file);
            new MineCloudDaemon(properties);

            properties = null;
            return;
        }

        Credentials mongo = new Credentials(properties.getProperty("mongo-hosts").split(";"),
                properties.getProperty("mongo-username"),
                properties.getProperty("mongo-password").toCharArray(),
                properties.getProperty("mongo-database"));
        Credentials redis = new Credentials(new String[] {properties.getProperty("redis-host")},
                properties.getProperty("redis-username"),
                properties.getProperty("redis-password").toCharArray());

        MineCloud.instance().initiateMongo(mongo);
        MineCloud.instance().initiateRedis(redis);

        new MineCloudDaemon(properties);
    }

    private static void runSetup(Properties properties, File file) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String[] hosts;
        String database;
        String username;
        String password;

        logger().info("I see you either have your details mis-configured or there is none, " +
                "we will proceed with performing the initial setup!");

        System.out.print("Please enter the hosts for MongoDB (separated by commas): ");
        hosts = scanner.nextLine().split(",");

        System.out.println();
        System.out.print("Please enter the database name: ");
        database = scanner.nextLine();

        System.out.println();
        System.out.print("Please enter the username for auth.:");
        username = scanner.nextLine();

        System.out.println();
        System.out.print("Please enter the password for " + username + ": ");
        password = scanner.nextLine();

        StringBuilder formattedHosts = new StringBuilder();

        for (String s : hosts) {
            formattedHosts.append(s)
                    .append(";");
        }

        properties.setProperty("mongo-hosts", formattedHosts.toString());
        properties.setProperty("mongo-database", database);
        properties.setProperty("mongo-username", username);
        properties.setProperty("mongo-password", password);

        Credentials mongo = new Credentials(hosts, username, password.toCharArray(), database);
        MineCloud.instance().initiateMongo(mongo);

        System.out.println();
        System.out.print("Great! Please enter the host for the Redis server: ");
        hosts = new String[] {scanner.nextLine()};

        System.out.println();
        System.out.print("Please enter the username for the Redis server: ");
        username = scanner.nextLine();

        System.out.println();
        System.out.print("Please enter the password for " + username + ":");

        password = scanner.nextLine();

        properties.setProperty("redis-host", hosts[0]);
        properties.setProperty("redis-username", username);
        properties.setProperty("redis-password", password);

        Credentials redis = new Credentials(hosts, username, password.toCharArray());
        MineCloud.instance().initiateRedis(redis);

        System.out.print("Lastly, please enter the name of this node: ");
        properties.setProperty("node-name", scanner.nextLine());


        System.out.println("Finished setup!");
        System.out.println("You can modify the database details in " + file.getAbsolutePath());

        properties.store(new FileOutputStream(file), "");
    }
}
