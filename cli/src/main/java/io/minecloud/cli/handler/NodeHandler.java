package io.minecloud.cli.handler;

import asg.cliche.Command;
import io.minecloud.MineCloud;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.nodes.type.NodeType;

import java.util.ArrayList;
import java.util.List;

public class NodeHandler extends AbstractHandler {
    Node node;

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
    public String privateIp(String ip) {
        node.setPrivateIp(ip);
        return "Set private ip of this node to " + ip + " successfully!";
    }

    @Command
    public String publicIp(String ip) {
        node.setPublicIp(ip);
        return "Set public ip of this node to " + ip + " successfully!";
    }

    @Command
    public String nodeType(String type) {
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
