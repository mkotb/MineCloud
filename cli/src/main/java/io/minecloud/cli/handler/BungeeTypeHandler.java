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
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.nodes.type.NodeType;
import io.minecloud.models.plugins.Plugin;
import io.minecloud.models.plugins.PluginType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BungeeTypeHandler extends AbstractHandler {
    BungeeType type;

    BungeeTypeHandler(String name) {
        type = MineCloud.instance().mongo()
                .repositoryBy(BungeeType.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating new one...");
            type = new BungeeType();

            type.setName(name);
        }
    }

    @Command
    public String dedicatedRam(@Param(name = "amount") int amount) {
        if (amount < 500) {
            return "Invalid ram amount!";
        }

        type.setDedicatedRam(amount);
        return "Set dedicated ram to " + amount + " MB successfully";
    }

    @Command
    public String preferredNode(@Param(name = "node-type-name") String nodeType) {
        NodeType type = MineCloud.instance().mongo()
                .repositoryBy(NodeType.class)
                .findFirst(nodeType);

        if (type == null) {
            return "No found node types by the name of " + nodeType;
        }

        this.type.setPreferredNode(type);
        return "Set preferred node type to " + nodeType;
    }

    @Command
    public String addPlugin(@Param(name = "plugin-name") String pluginName, @Param(name = "version") String version) {
        PluginType pluginType = MineCloud.instance().mongo()
                .repositoryBy(PluginType.class)
                .findFirst(pluginName);

        if (pluginType == null) {
            return "No found plugin by the name of " + pluginName;
        }

        if (!pluginType.versions().contains(version)) {
            return "No version by the name of " + version + " was found for " + pluginName;
        }

        if (type.plugins() == null) {
            type.setPlugins(new ArrayList<>());
        }

        List<Plugin> plugins = type.plugins();

        plugins.add(new Plugin(pluginType, version));
        type.setPlugins(plugins);

        return "Successfully added " + pluginName + "v" + version;
    }

    @Command
    public String removePlugin(@Param(name = "plugin-name") String pluginName) {
        Optional<Plugin> optional = type.plugins().stream()
                .filter((p) -> p.name().equalsIgnoreCase(pluginName))
                .findFirst();

        if (!optional.isPresent()) {
            return "No found plugin by the name of " + pluginName;
        }

        type.plugins().remove(optional.get());

        return "Removed plugin " + pluginName;
    }

    @Command
    public String push() {
        if (type.dedicatedRam() == 0 ||
                type.preferredNode() == null) {
            return "Required fields (dedicatedRam, preferredNode) have not been set by the user! " +
                    "Unable to push modifications";
        }

        MineCloud.instance().mongo()
                .repositoryBy(BungeeType.class)
                .save(type);
        return "Successfully pushed modifications to database!";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();
        list.add("Currently Modeling [Bungee Type] (" + type.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Dedicated Ram: " + type.dedicatedRam() + "MB");
        list.add("- Preferred Node: " + type.preferredNode().name());
        list.add("- Plugins: " + formatPlugins(type.plugins()));
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");
        return list;
    }
}
