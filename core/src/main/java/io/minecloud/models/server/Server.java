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

import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.db.mongo.model.MongoEntity;
import io.minecloud.db.redis.RedisDatabase;
import io.minecloud.db.redis.msg.binary.MessageOutputStream;
import io.minecloud.db.redis.pubsub.SimpleRedisChannel;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.player.PlayerData;
import io.minecloud.models.server.type.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * It is not recommended to play with the values of this class.
 *
 * Any inconsistent changes made to this class will be ignored.
 */
@Entity(value = "servers", noClassnameStored = true)
public class Server extends MongoEntity {
    @Reference(lazy = true)
    @Setter
    private Network network;
    @Reference(lazy = true)
    @Setter
    private ServerType type;
    @Reference(lazy = true)
    @Setter
    private Node node;
    @Setter
    private List<PlayerData> onlinePlayers;
    @Setter
    private int ramUsage;
    @Setter
    private int port;
    @Setter
    private int number;
    @Setter
    private String containerId;
    @Setter
    private double tps;
    @Setter
    private List<ServerMetadata> metadata;

    public Network network() {
        return network;
    }

    public ServerType type() {
        return type;
    }

    public Node node() {
        return node;
    }

    public List<PlayerData> onlinePlayers() {
        if (onlinePlayers == null) {
            onlinePlayers = new ArrayList<>();
        }

        return onlinePlayers;
    }

    public PlayerData playerBy(String name) {
        Optional<PlayerData> optional = onlinePlayers().stream()
                .filter((pd) -> pd.name().equals(name))
                .findFirst();

        return optional.isPresent() ? optional.get() : null;
    }

    public PlayerData playerBy(UUID id) {
        Optional<PlayerData> optional = onlinePlayers().stream()
                .filter((pd) -> pd.uuid().equals(id.toString()))
                .findFirst();

        return optional.isPresent() ? optional.get() : null;
    }

    public void removePlayer(UUID id) {
        onlinePlayers.remove(playerBy(id));
    }

    public int ramUsage() {
        return ramUsage;
    }

    public int port() {
        return port;
    }

    public int number() {
        return number;
    }

    public String containerId() {
        return containerId;
    }

    public double tps() {
        return tps;
    }

    public String name() {
        return entityId();
    }

    public List<ServerMetadata> metadata() {
        if (metadata == null) {
            metadata = new ArrayList<>();
        }

        return metadata;
    }

    public void addMetadata(ServerMetadata data) {
        metadata().removeIf((sm) -> sm.key().equals(data.key())); // override old entries
        metadata().add(data);
    }

    public Optional<ServerMetadata> metadataBy(String name) {
        return metadata().stream()
                .filter((sm) -> sm.key().equals(name))
                .findFirst();
    }

    public boolean hasMetadata(String name) {
        return metadataBy(name).isPresent();
    }

    public void teleport(String player) {
        RedisDatabase redis = MineCloud.instance().redis();

        if (redis.channelBy("teleport") == null) {
            redis.addChannel(SimpleRedisChannel.create("teleport", redis));
        }

        MessageOutputStream mos = new MessageOutputStream();

        try {
            mos.writeString(player);
            mos.writeString(name());
        } catch (IOException ex) {
            throw new MineCloudException("Could not encode teleport message", ex);
        }

        redis.channelBy("teleport").publish(mos.toMessage());
    }
}
