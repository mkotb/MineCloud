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
package io.minecloud.models.network;

import io.minecloud.MineCloud;
import io.minecloud.db.mongo.model.MongoEntity;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.bungee.type.BungeeTypeRepository;
import io.minecloud.models.network.server.ServerNetworkMetadata;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(value = "networks", noClassnameStored = true)
@EqualsAndHashCode(callSuper = true)
public class Network extends MongoEntity {
    @Setter
    private List<ServerNetworkMetadata> serverMetadata;
    @Setter
    private Map<String, Integer> bungees;
    @Setter
    @Reference
    private List<Node> nodes;

    public String name() {
        return entityId();
    }

    public List<ServerNetworkMetadata> serverMetadata() {
        return serverMetadata;
    }

    public Map<BungeeType, Integer> bungeeMetadata() {
        Map<BungeeType, Integer> metadata = new HashMap<>();

        if (bungees == null) {
            return metadata;
        }

        for (Map.Entry<String, Integer> entry : bungees.entrySet()) {
            BungeeTypeRepository repository = MineCloud.instance().mongo().repositoryBy(BungeeType.class);

            metadata.put(repository.findFirst(entry.getKey()), entry.getValue());
        }

        return metadata;
    }

    public List<Server> servers() {
        return ((ServerRepository) MineCloud.instance().mongo().repositoryBy(Server.class)).serversFor(this);
    }

    public int serversOnline() {
        return (int) MineCloud.instance().mongo().repositoryBy(Server.class)
                .count("network", this);
    }

    public int serversOnline(ServerType type) {
        ServerRepository repository = MineCloud.instance().mongo().repositoryBy(Server.class);

        return (int) repository.count(repository.createQuery()
                .field("network").equal(this)
                .field("type").equal(type));
    }

    public int bungeesOnline() {
        return (int) MineCloud.instance().mongo().repositoryBy(Bungee.class)
                .count("network", this);
    }

    public int bungeesOnline(BungeeType type) {
        BungeeRepository repository = MineCloud.instance().mongo().repositoryBy(Bungee.class);

        return (int) repository.count(repository.createQuery()
                .field("network").equal(this)
                .field("type").equal(type));
    }

    public List<Node> nodes() {
        return nodes;
    }

    public void setName(String name) {
        setId(name);
    }

    public void setBungees(Map<BungeeType, Integer> map) {
        bungees = new HashMap<>();

        for (Map.Entry<BungeeType, Integer> entry : map.entrySet()) {
            bungees.put(entry.getKey().name(), entry.getValue());
        }
    }
}
