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
import io.minecloud.models.nodes.type.NodeType;
import io.minecloud.models.plugins.Plugin;
import io.minecloud.models.plugins.PluginType;
import io.minecloud.models.server.World;
import io.minecloud.models.server.type.ServerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ServerTypeHandler extends AbstractHandler {
    private ServerType type;

    ServerTypeHandler(String name) {
        super();

        type = MineCloud.instance().mongo()
                .repositoryBy(ServerType.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating new one...");
            type = new ServerType();

            type.setName(name);
        }
    }

    @Command
    public String dedicatedRam(@Param(name = "amount (mb)") int amount) {
        if (amount < 500) {
            return "Invalid ram amount!";
        }

        type.setDedicatedRam(amount);
        return "Set dedicated ram to " + amount + " MB successfully";
    }

    @Command
    public String timeOut(@Param(name = "start up timeout (seconds)") int timeOut) {
        if (timeOut < 15) {
            return "Invalid time out! Timeout must be greater than or equal to 15!";
        }

        type.setTimeOut(timeOut);
        return "Set timeout to " + timeOut + "s successfully!";
    }

    @Command
    public String maxPlayers(@Param(name = "max-players") int max) {
        if (max < 0) {
            return "Invalid max players!";
        }

        type.setMaxPlayers(max);
        return "Set maximum amount of players to " + max + " successfully";
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
    public String mod(@Param(name = "mod") String mod) {
        type.setMod(mod);
        return "Set mod to " + mod;
    }

    @Command
    public String defaultServer(@Param(name = "value") boolean def) {
        type.setDefaultServer(def);
        return "Set default server value to " + def;
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

        plugins.add(new Plugin(pluginType, version, null));
        type.setPlugins(plugins);

        return "Successfully added " + pluginName + "v(" + version + ")";
    }

    @Command
    public String setPluginConfig(@Param(name = "plugin-name") String pluginName, @Param(name = "config") String config) {
        PluginType pluginType = MineCloud.instance().mongo()
                .repositoryBy(PluginType.class)
                .findFirst(pluginName);

        if (pluginType == null) {
            return "No found plugin by the name of " + pluginName;
        }

        if (!pluginType.configs().contains(config)) {
            return "No configs by the name of " + config + " was found!";
        }

        if (type.plugins() == null || !type.plugins().stream().anyMatch((p) -> p.name().equalsIgnoreCase(pluginName))) {
            return pluginName + " has not been added through add-plugin!";
        }

        type.plugins().stream()
                .filter((p) -> p.name().equalsIgnoreCase(pluginName))
                .findFirst().get()
                .setConfig(config);
        return "Successfully set config version to " + config;
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
    public String defaultWorld(@Param(name = "world-name") String world, @Param(name = "version") String version) {
        type.setDefaultWorld(new World(world, version));
        return "Set default world to " + world + " version " + version;
    }

    @Command(name = "add-world", abbrev = "aw")
    public String addWorld(@Param(name = "world-name") String world, @Param(name = "version") String version) {
        if (type.worlds() == null) {
            type.setWorlds(new ArrayList<>());
        }

        World wrld = new World(world, version);
        List<World> worlds = type.worlds();

        if (worlds.contains(wrld)) {
            return "World by the name of " + world + " already exists"; // don't worry, also considers versions
        }

        worlds.add(wrld);
        type.setWorlds(worlds);

        return "Added world " + world + " version " + version + " to the extra worlds";
    }

    @Command
    public String push() {
        if (type.dedicatedRam() == 0 ||
                type.maxPlayers() == 0 ||
                type.preferredNode() == null ||
                type.mod() == null ||
                type.defaultWorld() == null) {
            return "Required fields (dedicatedRam, maxPlayers, preferredNode, defaultWorld) have not been set by the user! " +
                    "Unable to push modifications";
        }

        MineCloud.instance().mongo()
                .repositoryBy(ServerType.class)
                .save(type);
        return "Successfully pushed modifications to database!";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();
        list.add("Currently Modeling [Server Type] (" + type.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Maximum Amount of Players: " + type.maxPlayers());
        list.add("- Preferred Node: " + type.preferredNode().name());
        list.add("- Mod: " + type.mod());
        list.add("- Dedicated Ram (Per Server): " + type.dedicatedRam() + "MB");
        list.add("- Plugins: " + formatPlugins(type.plugins()));
        list.add("- Is Default: " + (type.defaultServer() ? "Yes" : "No"));
        list.add("- Default World: " + type.defaultWorld().name() + "(" + type.defaultWorld().version() + ")");
        list.add("- Worlds: " + formatWorlds(type.worlds()));
        list.add("- Timeout: " + type.timeOut());
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");
        return list;
    }
}
