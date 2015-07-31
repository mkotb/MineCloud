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
import java.util.stream.Collectors;

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
    public String pluginServerType(@Param(name = "server-type") String name) {
        PluginServerType type = PluginServerType.valueOf(name.toUpperCase());

        if (type == null) {
            return "No server type of name " + name + " was found. " +
                    "Available server types: bungee, server";
        }

        this.type.setType(type); // typee asf
        return "Set plugin server type to " + name + " successfully";
    }

    @Command(name = "add-version", abbrev = "av")
    public String addVersion(@Param(name = "version-name") String version) {
        if (type.versions() == null) {
            type.setVersions(new ArrayList<>());
        }

        List<String> versions = type.versions();

        versions.add(version);
        type.setVersions(versions);

        return "Successfully added " + version + " as a plugin version";
    }

    @Command(name = "add-config", abbrev = "ac")
    public String addConfig(@Param(name = "config-name") String configName) {
        if (type.configs() == null) {
            type.setConfigs(new ArrayList<>());
        }

        List<String> configs = type.configs();

        configs.add(configName);
        type.setConfigs(configs);

        return "Successfully added " + configName + " as a config name";
    }

    @Command(name = "remove-version", abbrev = "rv")
    public String removeVersion(@Param(name = "version-name") String version) {
        if (type.versions() == null) {
            return "There are currently no versions!";
        }

        List<String> versions = type.versions();

        if (version.contains(version)) {
            versions.remove(version);
            type.setVersions(versions);

            return "Successfully removed " + version;
        }

        List<String> available = versions.stream()
                .filter((s) -> s.startsWith(version))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            return "No versions were found by the name of " + version;
        }

        if (available.size() == 1) {
            return removeVersion(available.get(0));
        }

        return removeVersion(optionPrompt(available));
    }

    @Command(name = "remove-config", abbrev = "rc")
    public String removeConfig(@Param(name = "config-name") String config) {
        if (type.configs() == null) {
            return "There are currently no configs!";
        }

        List<String> configs = type.configs();

        if (configs.contains(config)) {
            configs.remove(config);
            type.setVersions(configs);

            return "Successfully removed " + config;
        }

        List<String> available = configs.stream()
                .filter((s) -> s.startsWith(config))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            return "No configs were found by the name of " + config;
        }

        if (available.size() == 1) {
            return removeConfig(available.get(0));
        }

        return removeConfig(optionPrompt(available));
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
