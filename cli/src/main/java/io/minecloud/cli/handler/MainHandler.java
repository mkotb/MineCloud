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

public class MainHandler extends AbstractHandler {
    public MainHandler() {
        super();
    }

    @Command(name = "plugin-type")
    public void pluginType(@Param(name = "name") String name) {
        enterShell(new PluginTypeHandler(name), "plugin-type");
    }

    @Command(name = "server-type")
    public void serverType(@Param(name = "name") String name) {
        enterShell(new ServerTypeHandler(name), "server-type");
    }

    @Command(name = "node-type")
    public void nodeType(@Param(name = "name") String name) {
        enterShell(new NodeTypeHandler(name), "node-type");
    }

    @Command(name = "bungee-type")
    public void bungeeType(@Param(name = "name") String name) {
        enterShell(new BungeeTypeHandler(name), "bungee-type");
    }

    @Command(name = "network-type")
    public void networkType(@Param(name = "name") String name) {
        enterShell(new NetworkTypeHandler(name), "network-type");
    }

    @Command(name = "node")
    public void node(@Param(name = "name") String name) {
        enterShell(new NodeHandler(name), "node");
    }
}
