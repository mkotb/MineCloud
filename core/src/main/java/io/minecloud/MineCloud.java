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
package io.minecloud;

import io.minecloud.db.Credentials;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.bungee.type.BungeeTypeRepository;
import io.minecloud.models.network.Network;
import io.minecloud.models.network.NetworkRepository;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.NodeRepository;
import io.minecloud.models.nodes.type.NodeType;
import io.minecloud.models.nodes.type.NodeTypeRepository;
import io.minecloud.models.plugins.PluginType;
import io.minecloud.models.plugins.PluginTypeRepository;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import io.minecloud.models.server.type.ServerTypeRepository;
import lombok.Setter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

public final class MineCloud {
    private static final MineCloud INSTANCE = new MineCloud();
    private static final Logger LOGGER = Logger.getLogger("MineCloud");

    @Setter
    private MongoDatabase mongo;
    @Setter
    private RedisDatabase redis;

    private MineCloud() {}

    public static MineCloud instance() {
        return INSTANCE;
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static void environmentSetup() {
        Credentials mongoCredentials = new Credentials(System.getenv("mongo_hosts").split(";"),
                System.getenv("mongo_username"), System.getenv("mongo_password").toCharArray(),
                System.getenv("mongo_database"));
        Credentials redisCredentials = new Credentials(new String[] {System.getenv("redis_host")},
                System.getenv("redis_username"), System.getenv("redis_password").toCharArray());

        instance().initiateMongo(mongoCredentials);
        instance().initiateRedis(redisCredentials);
    }

    public static void runSetup(Properties properties, File file) throws IOException {
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


        logger().info("Finished setup!");
        logger().info("You can modify the database details in " + file.getAbsolutePath());

        properties.store(new FileOutputStream(file), "");
    }

    public MongoDatabase mongo() {
        return mongo;
    }

    public RedisDatabase redis() {
        return redis;
    }

    public void initiateMongo(Credentials credentials) {
        mongo = MongoDatabase.createDatabase(credentials);

        mongo.setup();

        mongo.loadRepository(NetworkRepository.create(mongo.datastore()), Network.class);
        mongo.loadRepository(NodeTypeRepository.create(mongo.datastore()), NodeType.class);
        mongo.loadRepository(NodeRepository.create(mongo.datastore()), Node.class);
        mongo.loadRepository(BungeeTypeRepository.create(mongo.datastore()), BungeeType.class);
        mongo.loadRepository(BungeeRepository.create(mongo.datastore()), Bungee.class);
        mongo.loadRepository(ServerTypeRepository.create(mongo.datastore()), ServerType.class);
        mongo.loadRepository(ServerRepository.create(mongo.datastore()), Server.class);
        mongo.loadRepository(PluginTypeRepository.create(mongo.datastore()), PluginType.class);
    }

    public void initiateRedis(Credentials credentials) {
        redis = RedisDatabase.create(credentials);

        redis.setup();
    }
}
