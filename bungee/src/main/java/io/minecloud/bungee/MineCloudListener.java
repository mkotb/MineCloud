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

import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.server.Server;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class MineCloudListener implements Listener {
    private MineCloudPlugin plugin;

    MineCloudListener(MineCloudPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        Bungee bungee = plugin.bungee();

        if (bungee == null)
            return;

        Collection<Server> servers = plugin.mongo.repositoryBy(Server.class)
                .findAll((s) -> s.network().name().equals(bungee.network().name()));

        int online = 0;
        int max = 0;

        for (Server server : servers) {
            online += server.onlinePlayers().size();
            max += server.type().maxPlayers();
        }

        ServerPing ping = event.getResponse();

        if (ping == null) {
            ping = new ServerPing();
        }

        ping.setPlayers(new ServerPing.Players(max, online, ping.getPlayers().getSample()));

        event.setResponse(ping);
    }

    @EventHandler
    public void serverKick(ServerKickEvent event) {
        String reason = event.getKickReason().toLowerCase();

        if (reason.contains("kick") || reason.contains("ban")) {
            return;
        }

        ServerInfo server = plugin.getProxy().getReconnectHandler().getServer(event.getPlayer());

        if (server != null) {
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        } else {
            return;
        }

        event.setCancelled(true);
        event.setCancelServer(server);
    }
}
