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

import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import io.minecloud.MineCloudException;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.plugins.Plugin;
import io.minecloud.models.server.World;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public abstract class AbstractHandler {
    private Shell current;

    protected AbstractHandler() {
    }

    public AbstractHandler(Shell shell) {
        this.current = shell;
    }

    public void enterShell(AbstractHandler subHandler, String name) {
        try {
            subHandler.current = ShellFactory.createSubshell(name, current, "minecloud", subHandler);
            subHandler.current.commandLoop();
        } catch (IOException ex) {
            throw new MineCloudException("Error encountered when in sub-command loop!", ex);
        }
    }

    public String optionPrompt(String... optins) {
        List<String> options = Arrays.asList(optins);

        System.out.println("Available options: (enter number)");

        for (int i = 0; i < options.size(); i++) {
            System.out.println(i + ": " + options.get(i));
        }

        int option = new Scanner(System.in).nextInt();

        if (option < 0 || option >= options.size()) {
            System.out.println("\nInvalid option! Trying again...\n\n");

            optionPrompt(optins);
        }

        return options.get(option);
    }

    public Shell currentShell() {
        return current;
    }

    public String formatPlugins(List<Plugin> plugins) {
        if (plugins == null || plugins.isEmpty())
            return "N/A";
        StringBuilder builder = new StringBuilder();

        for (Plugin plugin : plugins) {
            builder.append(plugin.name()).append("(v").append(plugin.version()).append("), ");
        }

        String toReturn = builder.toString().trim();
        toReturn = (toReturn.endsWith(",") ? toReturn.substring(0, toReturn.length() - 1) : toReturn);
        return toReturn;
    }

    public String formatStringList(List<String> versions) {
        if (versions == null || versions.isEmpty())
            return "N/A";
        StringBuilder builder = new StringBuilder();

        for (String version : versions) {
            builder.append(version).append(", ");
        }

        String toReturn = builder.toString().trim();
        toReturn = (toReturn.endsWith(",") ? toReturn.substring(0, toReturn.length() - 1) : toReturn);
        return toReturn;
    }

    public String formatWorlds(List<World> worlds) {
        if (worlds == null || worlds.isEmpty())
            return "N/A";
        StringBuilder builder = new StringBuilder();

        for (World world : worlds) {
            builder.append(world.name()).append("(v").append(world.version()).append("), ");
        }

        String toReturn = builder.toString().trim();
        toReturn = (toReturn.endsWith(",") ? toReturn.substring(0, toReturn.length() - 1) : toReturn);
        return toReturn;
    }

    public String formatBungees(Map<BungeeType, Integer> bungeeTypes) {
        if (bungeeTypes == null || bungeeTypes.isEmpty())
            return "N/A";

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<BungeeType, Integer> entry : bungeeTypes.entrySet()) {
            builder.append(entry.getKey().name())
                    .append(": ")
                    .append(entry.getValue())
                    .append(", ");
        }

        String toReturn = builder.toString().trim();
        toReturn = (toReturn.endsWith(",") ? toReturn.substring(0, toReturn.length() - 1) : toReturn);
        return toReturn;
    }

    public String formatNodes(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty())
            return "N/A";
        StringBuilder builder = new StringBuilder();

        for (Node node : nodes) {
            builder.append(node.name()).append("(").append(node.publicIp()).append(")").append(", ");
        }

        String toReturn = builder.toString().trim();
        toReturn = (toReturn.endsWith(",") ? toReturn.substring(0, toReturn.length() - 1) : toReturn);
        return toReturn;
    }
}
