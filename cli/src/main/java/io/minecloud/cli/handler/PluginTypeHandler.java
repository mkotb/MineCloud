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
import io.minecloud.models.plugins.PluginServerType;
import io.minecloud.models.plugins.PluginType;

import java.util.ArrayList;
import java.util.List;

public class PluginTypeHandler extends AbstractHandler {
    PluginType type;

    PluginTypeHandler(String name) {
        type = MineCloud.instance().mongo()
                .repositoryBy(PluginType.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating new one...");
            type = new PluginType();

            type.setName(name);
        }
    }

    @Command(name = "plugin-server-type", abbrev = "pst")
    public String pluginServerType(@Param(name = "Server Type") String name) {
        PluginServerType type = PluginServerType.valueOf(name.toUpperCase());

        if (type == null) {
            return "No server type of name " + name + " was found. " +
                    "Available server types: bungee, server";
        }

        this.type.setType(type); // typee asf
        return "Set plugin server type to " + name + " successfully";
    }

    @Command(name = "add-version", abbrev = "av")
    public String addVersion(String version) {
        if (type.versions() == null) {
            type.setVersions(new ArrayList<>());
        }

        List<String> versions = type.versions();

        versions.add(version);
        type.setVersions(versions);

        return "Successfully added " + version + " as a plugin version";
    }

    @Command(name = "add-config", abbrev = "ac")
    public String addConfig(String configName) {
        if (type.configs() == null) {
            type.setConfigs(new ArrayList<>());
        }

        List<String> configs = type.configs();

        configs.add(configName);
        type.setConfigs(configs);

        return "Successfully added " + configName + " as a config name";
    }

    @Command
    public String push() {
        if (type.type() == null) {
            return "Required fields (type) have not been set by the user! Unable to push modifications";
        }

        MineCloud.instance().mongo()
                .repositoryBy(PluginType.class)
                .save(type);
        return "Successfully pushed modifications to database!";
    }

    @Command(name = "!show")
    public List<String> show() {
        List<String> list = new ArrayList<>();
        list.add("Currently Modeling [Plugin Type] (" + type.name() + ")");
        list.add("===========================================");
        list.add("Listing Specifications...");
        list.add("- Server Type: " + type.type().name());
        list.add("- Versions: " + formatStringList(type.versions()));
        list.add("- Configs: " + formatStringList(type.configs()));
        list.add("===========================================");
        list.add("If you're ready to go, type 'push'.");
        return list;
    }
}
