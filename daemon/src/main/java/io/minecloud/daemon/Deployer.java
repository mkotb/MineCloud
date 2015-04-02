package io.minecloud.daemon;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import io.minecloud.MineCloud;
import io.minecloud.models.network.Network;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;

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
            MineCloud.logger().log(Level.ERROR, "Was unable to create server with type " + type.name());
            return;
        }

        server.setNetwork(network);
        server.setContainerId(creation.id());
        server.setNode(MineCloudDaemon.instance().node());
        server.setOnlinePlayers(new ArrayList<>());
        server.setNumber(repository.highestNumberFor(type) + 1);
        server.setRamUsage(0);

        repository.insert(server);
    }
}
