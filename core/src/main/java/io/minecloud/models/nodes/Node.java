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
package io.minecloud.models.nodes;

import io.minecloud.MineCloud;
import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;
import io.minecloud.models.nodes.type.NodeType;
import io.minecloud.models.server.Server;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@EqualsAndHashCode
public class Node implements MongoModel {
    @DataField
    @Setter
    private String name;
    @DataField
    @Setter
    private String publicIp;
    @DataField
    @Setter
    private String privateIp;
    @DataField
    @Setter
    private int ramDedicated;
    @DataField(reference = true)
    @Setter
    private NodeType type;
    @DataField
    @Setter
    private double availableRam;
    @DataField
    @Setter
    private List<CoreMetadata> coreMetadata;

    public String name() {
        return name;
    }

    public String privateIp() {
        return privateIp;
    }

    public String publicIp() {
        return publicIp;
    }

    public int ramDedicated() {
        return ramDedicated;
    }

    public NodeType type() {
        return type;
    }

    public List<CoreMetadata> coreMetadata() {
        return coreMetadata;
    }

    public double totalUsage() {
        double total = 0;

        for (int i = 0; i < coreMetadata.size(); i++) {
            total += usage(i);
        }

        return total;
    }

    public double usage(int core) {
        if (core >= coreMetadata.size()) {
            return -1;
        }

        return coreMetadata.get(core).usage();
    }

    public double availableRam() {
        return availableRam;
    }

    public List<Server> servers() {
        return MineCloud.instance().mongo().repositoryBy(Server.class).models()
                .stream()
                .filter((server) -> server.node().equals(this))
                .collect(Collectors.toList());
    }

    public int serverCount() {
        return servers().size();
    }
}
