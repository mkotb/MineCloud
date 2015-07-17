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
package io.minecloud.models.server;

import io.minecloud.db.mongo.AbstractMongoRepository;
import io.minecloud.models.network.Network;
import io.minecloud.models.player.PlayerData;
import io.minecloud.models.server.type.ServerType;
import org.mongodb.morphia.Datastore;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ServerRepository extends AbstractMongoRepository<Server> {
    private ServerRepository(Datastore datastore) {
        super(Server.class, datastore);
    }

    public static ServerRepository create(Datastore datastore) {
        return new ServerRepository(datastore);
    }

    public int nextNumberFor(ServerType type) {
        List<Server> servers = find(createQuery().filter("type", type)).asList();
        int lastNumber = 0;

        Collections.sort(servers, (s1, s2) -> s1.number() - s2.number());

        for (Server server : servers) {
            if (lastNumber != (server.number() - 1))
                return server.number() - 1;

            lastNumber = server.number();
        }

        return 1;
    }

    public Server serverBy(ServerType type, int number) {
        return find(createQuery()
                .field("type").equal(type)
                .field("number").equal(number))
                .get();
    }

    public List<Server> serverBy(ServerType type) {
        return find(createQuery().field("type").equal(type))
                .asList();
    }

    public List<Server> serversFor(Network network) {
        return find(createQuery().field("network").equal(network))
                .asList();
    }

    public Server serverFor(UUID id) {
        PlayerData data = new PlayerData();

        data.setId(id.toString());
        return find(createQuery().field("players").hasThisElement(data))
                .get();
    }

    public Server serverFor(String name) {
        PlayerData data = new PlayerData();

        data.setName(name);
        return find(createQuery().field("players").hasThisElement(data))
                .get();
    }
}
