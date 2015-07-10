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
package io.minecloud.bungee;

import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.List;

public class ReconnectHandler extends AbstractReconnectHandler {
    private MineCloudPlugin plugin;

    ReconnectHandler(MineCloudPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected ServerInfo getStoredServer(ProxiedPlayer proxiedPlayer) {
        ServerInfo info = ReconnectHandler.getForcedHost(proxiedPlayer.getPendingConnection());

        if (info == null) {
            ServerRepository repository = plugin.mongo.repositoryBy(Server.class);
            List<Server> servers = repository.find(repository.createQuery()
                    .field("network").equal(plugin.bungee().network()))
                    .asList();

            servers.removeIf((s) -> !s.type().defaultServer());

            Collections.sort(servers, (a, b) -> b.onlinePlayers().size() - a.onlinePlayers().size());

            info = plugin.getProxy().getServerInfo(servers.get(0).name());
        }

        if (info == null) {
            proxiedPlayer.disconnect(new TextComponent("Could not find a server to connect to, please report."));
        }

        return info;
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {

    }

    @Override
    public void save() {
    }

    @Override
    public void close() {
    }
}
