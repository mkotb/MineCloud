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
package io.minecloud.cli.handler;

import asg.cliche.Command;
import asg.cliche.Param;
import io.minecloud.MineCloud;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.type.NodeType;

import java.util.ArrayList;
import java.util.List;

class NodeHandler extends AbstractHandler {
    private Node node;

    NodeHandler(String name) {
        super();

        node = MineCloud.instance().mongo()
                .repositoryBy(Node.class)
                .findFirst(name);

        if (node == null) {
            System.out.println("Could not find type in database; creating new one...");
            node = new Node();

            node.setName(name);
            node.setAvailableRam(2L); // 2 mb ftw
        }
    }

    @Command
    public String privateIp(@Param(name = "private-ip") String ip) {
        node.setPrivateIp(ip);
        return "Set private ip of this node to " + ip + " successfully!";
    }

    @Command
    public String publicIp(@Param(name = "public-ip") String ip) {
        node.setPublicIp(ip);
        return "Set public ip of this node to " + ip + " successfully!";
    }

    @Command
    public String nodeType(@Param(name = "node-type-name") String type) {
        NodeType nt = MineCloud.instance().mongo()
                .repositoryBy(NodeType.class)
                .findFirst(type);

        if (nt == null) {
            return "No node type by the name of " + type + " exists!";
        }

        node.setType(nt);
        return "Successfully set the node type to " + type + "!";
    }

    @Command
    public String push() {
        if (node.publicIp() == null ||
                node.privateIp() == null ||
                node.type() == null) {
            return "Required field (publicIp, privateIp, nodeType) have not been set by the user! " +
                    "Unable to push modifications";
        }

        MineCloud.instance().mongo()
                .repositoryBy(Node.class)
                .save(node);
        return "Successfully pushed modifications to database";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();

        list.add("Currently Modeling [Node] (" + node.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Public IP: " + node.publicIp());
        list.add("- Private IP: " + node.privateIp());
        list.add("- Node Type: " + (node.type() == null ? "N/A" : node.type().name()));
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");

        return list;
    }
}
