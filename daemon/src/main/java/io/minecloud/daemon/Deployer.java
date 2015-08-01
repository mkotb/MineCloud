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
package io.minecloud.daemon;

import com.spotify.docker.client.ContainerNotFoundException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.*;
import io.minecloud.MineCloud;
import io.minecloud.db.Credentials;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerMetadata;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.World;
import io.minecloud.models.server.type.ServerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class Deployer {
    private static final AtomicInteger FAILED_STARTS = new AtomicInteger(0);

    private Deployer() {}

    public static void deployServer(Network network, ServerType type, List<ServerMetadata> metadata) {
        ServerRepository repository = MineCloud.instance().mongo().repositoryBy(Server.class);
        Server server = new Server();

        server.setType(type);
        server.setNumber(repository.nextNumberFor(type));
        server.setNetwork(network);
        server.setNode(MineCloudDaemon.instance().node());
        server.setOnlinePlayers(new ArrayList<>());
        server.setRamUsage(-1);
        server.setPort(-1);
        server.setContainerId(server.type().name() + server.number());
        server.setId(server.containerId());
        server.setMetadata(metadata);

        repository.save(server);

        new Thread(() -> {
            boolean deployed = true;

            for (int i = 0; i < 3 && !deployServer(server); i++) {
                deployed = i != 2;
            }

            if (!deployed) {
                failedStart(network);
                repository.deleteById(server.entityId());
            }
        }, server.name() + " creator").start();
    }

    public static boolean deployServer(Server server) {
        Credentials mongoCreds = MineCloud.instance().mongo().credentials();
        Credentials redisCreds = MineCloud.instance().redis().credentials();
        World defaultWorld = server.type().defaultWorld();
        ContainerConfig config = ContainerConfig.builder()
                .image("minecloud/server")
                .openStdin(true)
                .env(new EnvironmentBuilder()
                        .append("mongo_hosts", mongoCreds.formattedHosts())
                        .append("mongo_username", mongoCreds.username())
                        .append("mongo_password", new String(mongoCreds.password()))
                        .append("mongo_database", mongoCreds.database())

                        .append("redis_host", redisCreds.hosts()[0])
                        .append("redis_username", redisCreds.username())
                        .append("redis_password", new String(redisCreds.password()))
                        .append("SERVER_MOD", server.type().mod())
                        .append("DEDICATED_RAM", String.valueOf(server.type().dedicatedRam()))
                        .append("MAX_PLAYERS", String.valueOf(server.type().maxPlayers()))

                        .append("server_id", server.entityId())
                        .append("DEFAULT_WORLD", defaultWorld.name())
                        .append("DEFAULT_WORLD_VERSION", defaultWorld.version())
                        .build())
                .build();

        ContainerCreation creation;

        try {
            DockerClient client = MineCloudDaemon.instance().dockerClient();
            String name = server.type().name() + server.number();

            try {
                ContainerInfo info = client.inspectContainer(name);

                if (info.state().running()) {
                    client.killContainer(name);
                }

                client.removeContainer(info.id());
            } catch (ContainerNotFoundException ignored) {}

            creation = client.createContainer(config, name);

            client.startContainer(creation.id(), HostConfig.builder()
                    .binds("/mnt/minecloud:/mnt/minecloud")
                    .publishAllPorts(true)
                    .build());
        } catch (InterruptedException | DockerException ex) {
            MineCloud.logger().log(Level.SEVERE, "Was unable to create server with type " + server.type().name(),
                    ex);
            return false;
        }

        MineCloud.logger().info("Started server " + server.name()
                + " with container id " + server.containerId());
        return true;
    }

    public static void deployBungee(Network network, BungeeType type) {
        Bungee bungee;

        for (int i = 0; i < 3 && ((bungee = deployBungeeCord(network, type)) == null || bungee.network() == null); i++) {
        }
    }

    public static Bungee deployBungeeCord(Network network, BungeeType type) {
        BungeeRepository repository = MineCloud.instance().mongo().repositoryBy(Bungee.class);
        Node node = MineCloudDaemon.instance().node();
        Bungee bungee = new Bungee();

        if (repository.count("_id", node.publicIp()) > 0) {
            MineCloud.logger().log(Level.WARNING, "Did not create bungee on this node; public ip is already in use");
            return null;
        }

        bungee.setId(node.publicIp());

        Credentials mongoCreds = MineCloud.instance().mongo().credentials();
        Credentials redisCreds = MineCloud.instance().redis().credentials();
        ContainerConfig config = ContainerConfig.builder()
                .image("minecloud/bungee")
                .exposedPorts("25565")
                .openStdin(true)
                .env(new EnvironmentBuilder()
                        .append("mongo_hosts", mongoCreds.formattedHosts())
                        .append("mongo_username", mongoCreds.username())
                        .append("mongo_password", new String(mongoCreds.password()))
                        .append("mongo_database", mongoCreds.database())

                        .append("redis_host", redisCreds.hosts()[0])
                        .append("redis_username", redisCreds.username())
                        .append("redis_password", new String(redisCreds.password()))
                        .append("DEDICATED_RAM", String.valueOf(type.dedicatedRam()))

                        .append("bungee_id", node.publicIp())
                        .build())
                .build();
        HostConfig hostConfig = HostConfig.builder()
                .binds("/mnt/minecloud:/mnt/minecloud")
                .portBindings(new HashMap<String, List<PortBinding>>() {{
                    put("25565", Arrays.asList(PortBinding.of(node.publicIp(), 25565))); // I'm sorry
                }})
                .publishAllPorts(true)
                .build();

        ContainerCreation creation;

        try {
            DockerClient client = MineCloudDaemon.instance().dockerClient();
            creation = client.createContainer(config, type.name());

            client.startContainer(creation.id(), hostConfig);
        } catch (InterruptedException | DockerException ex) {
            MineCloud.logger().log(Level.SEVERE, "Was unable to create bungee with type " + type.name(),
                    ex);
            return bungee;
        }

        bungee.setNetwork(network);
        bungee.setNode(node);
        bungee.setPublicIp(node.publicIp());
        bungee.setType(type);

        repository.save(bungee);
        MineCloud.logger().info("Started bungee " + bungee.name() + " with container id " + bungee.containerId());
        return bungee;
    }

    private static void failedStart(Network network) {
        if (FAILED_STARTS.incrementAndGet() < 3) {
            MineCloud.logger().warning("Failed to start a container on " + network.name() + ", adding to failed starts...");
            return;
        }

        List<Node> nodes = network.nodes();

        nodes.removeIf((node) -> node.name().equalsIgnoreCase(System.getenv("node-name")));
        network.setNodes(nodes);

        MineCloud.instance().mongo().repositoryBy(Network.class).save(network);
        MineCloud.logger().log(Level.SEVERE, "Failed to create containers 3 times! Removing node from network...");
    }

    private static class EnvironmentBuilder {
        private List<String> environmentVars = new ArrayList<>();

        private EnvironmentBuilder() {
        }

        public EnvironmentBuilder append(String key, String value) {
            environmentVars.add(key + "=" + value);
            return this;
        }

        public String[] build() {
            return environmentVars.stream().toArray(String[]::new);
        }
    }
}
