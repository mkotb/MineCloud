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

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.player.PlayerData;
import io.minecloud.models.server.type.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.List;

/*
 * It is not recommended to play with the values of this class.
 *
 * Any inconsistent changes made to this class will be ignored.
 */
@EqualsAndHashCode
public class Server implements MongoModel {
    @DataField(reference = true)
    @Setter
    private Network network;
    @DataField(reference = true)
    @Setter
    private ServerType type;
    @DataField(reference = true)
    @Setter
    private Node node;
    @DataField
    @Setter
    private List<PlayerData> onlinePlayers;
    @DataField
    @Setter
    private int ramUsage;
    @DataField
    @Setter
    private int port;
    @DataField
    @Setter
    private int number;
    @DataField
    @Setter
    private String containerId;

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
        return onlinePlayers;
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
}
