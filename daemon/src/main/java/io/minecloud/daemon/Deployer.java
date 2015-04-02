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

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import io.minecloud.MineCloud;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class Deployer {

    private Deployer() {}

    public static void deployServer(Network network, ServerType type) {
        ServerRepository repository = MineCloud.instance().mongo().repositoryBy(Server.class);
        Server server = new Server();
        ContainerConfig config = ContainerConfig.builder()
                .image("minecloud/server")
                .exposedPorts("25565")
                .openStdin(true)
                .env("") // TODO ENVs
                .cmd("sh initialize.sh") // TODO
                .build();

        ContainerCreation creation;

        try {
            DockerClient client = MineCloudDaemon.instance().dockerClient();
            creation = client.createContainer(config);

            client.startContainer(creation.id());
        } catch (InterruptedException | DockerException ex) {
            MineCloud.logger().log(Level.ERROR, "Was unable to create server with type " + type.name(),
                    ex);
            return;
        }

        server.setNetwork(network);
        server.setContainerId(creation.id());
        server.setNode(MineCloudDaemon.instance().node());
        server.setOnlinePlayers(new ArrayList<>());
        server.setNumber(repository.highestNumberFor(type) + 1);
        server.setRamUsage(-1);

        repository.insert(server);
    }

    public static void deployBungee(Network network, BungeeType type) {
        BungeeRepository repository = MineCloud.instance().mongo().repositoryBy(Bungee.class);
        Node node = MineCloudDaemon.instance().node();
        Bungee bungee = new Bungee();
        ContainerConfig config = ContainerConfig.builder()
                .image("minecloud/bungee")
                .exposedPorts("25565")
                .openStdin(true)
                .env("") // TODO ENVs
                .cmd("sh initialize.sh")
                .build();
        HostConfig hostConfig = HostConfig.builder()
                .portBindings(new HashMap<String, List<PortBinding>>() {{
                    put("25565", Arrays.asList(PortBinding.of(node.publicIp(), 25565))); // I'm sorry
                }})
                .build();

        ContainerCreation creation;

        try {
            DockerClient client = MineCloudDaemon.instance().dockerClient();
            creation = client.createContainer(config);

            client.startContainer(creation.id(), hostConfig);
        } catch (InterruptedException | DockerException ex) {
            MineCloud.logger().log(Level.ERROR, "Was unable to create bungee with type " + type.name(),
                    ex);
            return;
        }

        bungee.setNetwork(network);
        bungee.setContainerId(creation.id());
        bungee.setNode(node);
        bungee.setPublicIp(node.publicIp());
        bungee.setType(type);
        bungee.setRamUsage(-1);

        repository.insert(bungee);
    }
}
