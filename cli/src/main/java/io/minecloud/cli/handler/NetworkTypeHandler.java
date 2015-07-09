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
import io.minecloud.MineCloud;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.network.server.ServerMetadata;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.type.ServerType;

import java.util.*;

public class NetworkTypeHandler extends AbstractHandler {
    Network type;

    NetworkTypeHandler(String name) {
        type = MineCloud.instance().mongo()
                .repositoryBy(Network.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating a new one...");
            type = new Network();

            type.setName(name);
        }
    }

    @Command
    public String addBungee(String bungeeName, int amount) {
        BungeeType bungeeType = MineCloud.instance().mongo()
                .repositoryBy(BungeeType.class)
                .findFirst(bungeeName);

        if (bungeeType == null) {
            return "No bungees found by the name of " + bungeeName;
        }

        if (type.bungeeMetadata() == null) {
            type.setBungees(new HashMap<>());
        }

        Map<BungeeType, Integer> bungeeTypes = type.bungeeMetadata();

        bungeeTypes.put(bungeeType, amount);
        type.setBungees(bungeeTypes);

        return "Successfully added " + bungeeName + " to Network";
    }

    @Command
    public String addServer(String name, int min, int max) {
        ServerType serverType = MineCloud.instance().mongo()
                .repositoryBy(ServerType.class)
                .findFirst(name);

        if (serverType == null) {
            return "No server types found by the name of " + name;
        }

        if (type.serverMetadata() == null) {
            type.setServerMetadata(new ArrayList<>());
        }

        List<ServerMetadata> metadata = type.serverMetadata();
        ServerMetadata md = new ServerMetadata();

        md.setType(serverType);
        md.setMaximumAmount(max);
        md.setMaximumAmount(min);

        metadata.add(md);

        return "Successfully added " + name + " to Network!";
    }

    @Command
    public String addNode(String nodeName) {
        Node node = MineCloud.instance().mongo()
                .repositoryBy(Node.class)
                .findFirst(nodeName);

        if (node == null) {
            return "No nodes found by the name of " + nodeName;
        }

        if (type.nodes() == null) {
            type.setNodes(new ArrayList<>());
        }

        List<Node> nodes = type.nodes();

        nodes.add(node);
        type.setNodes(nodes);

        return "Successfully added " + nodeName + " to Network";
    }

    @Command
    public String removeBungee(String bungee) {
        BungeeType bungeeType = MineCloud.instance().mongo()
                .repositoryBy(BungeeType.class)
                .findFirst(bungee);

        if (bungeeType == null) {
            return "No bungees found by the name of " + bungee;
        }

        if (type.bungeeMetadata() == null) {
            type.setBungees(new HashMap<>());
        }

        Map<BungeeType, Integer> bungeeTypes = type.bungeeMetadata();

        if (!bungeeTypes.containsKey(bungeeType)) {
            return bungee + " is not on the network";
        }

        bungeeTypes.remove(bungeeType);
        return bungee + " has been removed from the network!";
    }

    @Command
    public String removeServer(String name) {
        ServerType serverType = MineCloud.instance().mongo()
                .repositoryBy(ServerType.class)
                .findFirst(name);

        if (serverType == null) {
            return "No server types found by the name of " + name;
        }

        if (type.serverMetadata() == null) {
            type.setServerMetadata(new ArrayList<>());
        }

        List<ServerMetadata> metadata = type.serverMetadata();
        Optional<ServerMetadata> optional = metadata.stream()
                .filter((sm) -> sm.type().equals(serverType))
                .findFirst();

        if (!optional.isPresent()) {
            return name + " is not on the network!";
        }

        metadata.remove(optional.get());
        return name + " has been removed from the network!";
    }

    @Command
    public String removeNode(String nodeName) {
        Node node = MineCloud.instance().mongo()
                .repositoryBy(Node.class)
                .findFirst(nodeName);

        if (node == null) {
            return "No nodes found by the name of " + nodeName;
        }

        if (type.nodes() == null) {
            type.setNodes(new ArrayList<>());
        }

        List<Node> nodes = type.nodes();

        if (!nodes.contains(node)) {
            return nodeName + " is not on the network!";
        }

        nodes.remove(node);
        return nodeName + " has been removed from the network!";
    }

    @Command
    public String push() {
        if (type.bungeeMetadata() == null || type.nodes() == null) {
            return "Required fields (bungee, nodes) have not been set by the user! Unable to push modifications";
        }

        MineCloud.instance().mongo().repositoryBy(Network.class).save(type);

        return "Successfully pushed modifications to database!";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();
        list.add("Currently Modeling [Network Type] (" + type.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Bungees: " + formatBungees(type.bungeeMetadata()));
        list.add("- Nodes: " + formatNodes(type.nodes()));
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");
        return list;
    }

}
