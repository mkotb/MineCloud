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
package io.minecloud.db.mongo;

import com.mongodb.*;
import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.db.Credentials;
import io.minecloud.db.Database;
import io.minecloud.db.mongo.model.MongoEntity;
import org.apache.logging.log4j.Level;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.net.UnknownHostException;
import java.util.*;

public class MongoDatabase implements Database {
    private final Map<Class<?>, MongoRepository<?>> repositories = new HashMap<>();
    private final Credentials credentials;
    private DB database;
    private Morphia morphia;
    private Datastore datastore;

    private MongoDatabase(Credentials credentials) {
        this.credentials = credentials;
    }

    public static MongoDatabase createDatabase(Credentials credentials) {
        return new MongoDatabase(credentials);
    }

    public <T extends MongoEntity, E extends AbstractMongoRepository<T>> E repositoryBy(Class<T> model) {
        return (E) repositories.get(model);
    }

    public <T> void loadRepository(MongoRepository<T> repository, Class<T> cls) {
        repositories.put(cls, repository);
    }

    public DB db() {
        return database;
    }

    public Morphia morphia() {
        return morphia;
    }

    public Datastore datastore() {
        return datastore;
    }

    public Credentials credentials() {
        return credentials;
    }

    @Override
    public void setup() {
        MongoClientOptions options = MongoClientOptions.builder().connectionsPerHost(10000)
                .heartbeatConnectRetryFrequency(15)
                .heartbeatConnectTimeout(10)
                .heartbeatFrequency(10)
                .heartbeatThreadCount(1)
                .build();
        List<ServerAddress> hosts = new ArrayList<>();

        for (String host : credentials.hosts()) {
            try {
                hosts.add(new ServerAddress(host));
            } catch (UnknownHostException exception) {
                MineCloud.logger().warn(host + " caused a UnknownHostException: " + exception.getMessage());
            }
        }

        if (hosts.size() == 0) {
            MineCloud.logger().log(Level.FATAL, "No viable host was found for MongoDB!", new MineCloudException());
            return;
        }

        MongoClient client;

        if (credentials.username() != null && !credentials.username().equalsIgnoreCase("")) {
            MongoCredential credential = MongoCredential.createMongoCRCredential(credentials.username(),
                    credentials.database(), credentials.password());

            client = new MongoClient(hosts, Arrays.asList(credential), options);
        } else {
            client = new MongoClient(hosts, options);
        }

        if (hosts.size() > 1) {
            client.setWriteConcern(WriteConcern.REPLICA_ACKNOWLEDGED);
            client.setReadPreference(ReadPreference.nearest());
        }

        database = client.getDB(credentials.database());
        morphia = new Morphia();
        datastore = morphia.createDatastore(client, credentials.database());
    }
}
