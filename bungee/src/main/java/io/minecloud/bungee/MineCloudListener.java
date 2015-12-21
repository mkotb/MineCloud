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
import io.minecloud.models.server.ServerRepository;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class MineCloudListener implements Listener {
    private long lastUpdated = 0;
    private int onlinePlayers = -1;
    private int maxOnline = -1;
    private MineCloudPlugin plugin;

    MineCloudListener(MineCloudPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        int online = 0;
        int max = 0;

        if (ping == null) {
            ping = new ServerPing();
        }

        if (onlinePlayers != -1 && (System.currentTimeMillis() - lastUpdated) < 5000L) {
            online = onlinePlayers;
            max = maxOnline;
        } else {
            Bungee bungee = plugin.bungee();

            if (bungee == null)
                return;

            ServerRepository repository = plugin.mongo.repositoryBy(Server.class);
            Collection<Server> servers = repository.find(repository.createQuery().field("network").equal(bungee.network()))
                    .asList();

            for (Server server : servers) {
                online += server.onlinePlayers().size();
                max += server.type().maxPlayers();
            }

            onlinePlayers = online;
            maxOnline = max;
            lastUpdated = System.currentTimeMillis();
        }

        ping.setPlayers(new ServerPing.Players(max, online, ping.getPlayers().getSample()));

        event.setResponse(ping);
    }

    @EventHandler
    public void serverKick(ServerKickEvent event) {
        String reason = event.getKickReason().toLowerCase();

        if (reason.contains("kick") || reason.contains("ban") || reason.contains("pack")) {
            event.getPlayer().disconnect(event.getKickReasonComponent());
            event.setCancelled(false);
            return;
        }

        ServerInfo server = plugin.getProxy().getReconnectHandler().getServer(event.getPlayer());

        if (server != null) {
            event.getPlayer().sendMessage(event.getKickReasonComponent());
        } else {
            return;
        }

        event.getPlayer().connect(server);
        event.setCancelServer(server);
        event.setCancelled(true);
    }
}
