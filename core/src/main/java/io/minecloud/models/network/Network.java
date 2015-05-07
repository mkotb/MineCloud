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
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.bungee.BungeeMetadata;
import io.minecloud.models.network.server.ServerMetadata;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.type.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

@Entity(value = "networks", noClassnameStored = true)
@EqualsAndHashCode
public class Network extends MongoEntity {
    @Setter
    private List<ServerMetadata> serverMetadata;
    @Setter
    private List<BungeeMetadata> bungeeMetadata;
    @Setter
    private List<String> nodes;

    public String name() {
        return entityId();
    }

    public List<ServerMetadata> serverMetadata() {
        return serverMetadata;
    }

    public List<BungeeMetadata> bungeeMetadata() {
        return bungeeMetadata;
    }

    public int serversOnline() {
        return (int) MineCloud.instance().mongo().repositoryBy(Server.class).models()
                .stream()
                .filter((server) -> server.network().equals(this))
                .count();
    }

    public int serversOnline(ServerType type) {
        return (int) MineCloud.instance().mongo().repositoryBy(Server.class).models()
                .stream()
                .filter((server) -> server.network().equals(this) && server.type().equals(type))
                .count();
    }

    public int bungeesOnline() {
        return (int) MineCloud.instance().mongo().repositoryBy(Bungee.class).models()
                .stream()
                .filter((bungee) -> bungee.network().equals(this))
                .count();
    }

    public int bungeesOnline(BungeeType type) {
        return (int) MineCloud.instance().mongo().repositoryBy(Bungee.class).models()
                .stream()
                .filter((bungee) -> bungee.network().equals(this) && bungee.type().equals(type))
                .count();
    }

    public List<String> nodes() {
        return nodes;
    }
}
