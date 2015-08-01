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
package io.minecloud.bukkit;

import io.minecloud.models.player.PlayerData;
import io.minecloud.models.server.Server;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlayerTracker implements Listener {
    private MineCloudPlugin plugin;

    PlayerTracker() {
        this.plugin = (MineCloudPlugin) JavaPlugin.getProvidingPlugin(PlayerTracker.class);
    }

    private void updatePlayer(Server server, PlayerData data) {
        List<PlayerData> onlinePlayers = server.onlinePlayers();

        onlinePlayers.set(onlinePlayers.lastIndexOf(data), data);
        server.setOnlinePlayers(onlinePlayers);

        plugin.mongo().repositoryBy(Server.class).save(server);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Server server = plugin.server();
            List<PlayerData> onlinePlayers = server.onlinePlayers();
            Player player = event.getPlayer();
            PlayerData data = new PlayerData();

            data.setHealth(player.getHealth());
            data.setMaxHealth(player.getMaxHealth());
            data.setName(player.getName());
            data.setId(player.getUniqueId().toString());

            onlinePlayers.add(data);

            server.setOnlinePlayers(onlinePlayers);
            plugin.mongo().repositoryBy(Server.class).save(server);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void playerHit(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player player = (Player) event.getEntity();
            Server server = plugin.server();
            PlayerData data = server.playerBy(player.getUniqueId());

            if (data == null)
                return;

            data.setHealth(player.getHealth());
            data.setMaxHealth(player.getMaxHealth());

            updatePlayer(server, data);
        });
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Server server = plugin.server();

            server.removePlayer(event.getPlayer().getUniqueId());
            plugin.mongo().repositoryBy(Server.class).save(server);
        });
    }
}
