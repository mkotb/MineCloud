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
import io.minecloud.models.network.Network;
import io.minecloud.models.network.NetworkRepository;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.NodeRepository;
import io.minecloud.models.nodes.type.NodeType;
import io.minecloud.models.nodes.type.NodeTypeRepository;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import io.minecloud.models.server.type.ServerTypeRepository;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MineCloud {
    private static final MineCloud INSTANCE = new MineCloud();
    private static final Logger LOGGER = LogManager.getLogger();

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

    public MongoDatabase mongo() {
        return mongo;
    }

    public RedisDatabase redis() {
        return redis;
    }

    public void initiateMongo(Credentials credentials) {
        mongo = MongoDatabase.createDatabase(credentials);

        mongo.setup();

        mongo.loadRepository(NetworkRepository.create(mongo), Network.class);
        mongo.loadRepository(NodeTypeRepository.create(mongo), NodeType.class);
        mongo.loadRepository(NodeRepository.create(mongo), Node.class);
        mongo.loadRepository(ServerTypeRepository.create(mongo), ServerType.class);
        mongo.loadRepository(ServerRepository.create(mongo), Server.class);
    }

    public void initiateRedis(Credentials credentials) {
        redis = RedisDatabase.create(credentials);

        redis.setup();
    }
}
