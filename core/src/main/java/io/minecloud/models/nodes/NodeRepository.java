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

import io.minecloud.db.mongo.AbstractMongoRepository;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.type.NodeType;
import org.mongodb.morphia.Datastore;

public class NodeRepository extends AbstractMongoRepository<Node> {
    private NodeRepository(Datastore datastore) {
        super(Node.class, datastore);
    }

    public static NodeRepository create(Datastore datstore) {
        return new NodeRepository(datstore);
    }

    public Node nodeBy(String name) {
        return findFirst((node) -> node.name().equalsIgnoreCase(name));
    }

    public Node findNode(Network network, NodeType preferredNode, int requiredRam) {
        Node selectedNode = null;

        for (Node node : network.nodes()) {
            double nodeAllocated = node.allocatedRam();
            double selectedNodeAllocated = selectedNode == null ? 0 : selectedNode.allocatedRam();

            if (selectedNode == null && node.availableRam() >= requiredRam) {
                selectedNode = node;
                continue;
            }

            if (selectedNode == null) {
                continue;
            }

            double ramDifference = nodeAllocated - selectedNodeAllocated;

            if (ramDifference > 0) {
                double usageDifference = selectedNode.totalUsage() - node.totalUsage();

                if (usageDifference > 0) {
                    selectedNode = node;
                } else if (ramDifference >= (requiredRam * 1.5) ||
                        (usageDifference >= -125 && isPreferredNode(node, selectedNode, preferredNode))) {
                    selectedNode = node;
                }
            } else if (ramDifference >= -nodeMemoryThreshold(node) &&
                    isPreferredNode(node, selectedNode, preferredNode)) {
                selectedNode = node;
            }
        }

        return selectedNode;
    }

    private double nodeMemoryThreshold(Node node) {
        return (node.availableRam() / (node.serverCount() + 1));
    }

    private boolean isPreferredNode(Node node, Node currentNode, NodeType preferred) {
        return node.type().equals(preferred) && !currentNode.type().equals(preferred);
    }
}
