package io.minecloud.daemon;

import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.db.Credentials;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.BungeeRepository;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.Server;
import io.minecloud.models.server.ServerMetadata;
import io.minecloud.models.server.ServerRepository;
import io.minecloud.models.server.type.ServerType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class Deployer {
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(32812);

    private Deployer() {
    }

    public static void deployServer(Network network, ServerType type, List<ServerMetadata> metadata) {
        Credentials mongoCreds = MineCloud.instance().mongo().credentials();
        Credentials redisCreds = MineCloud.instance().redis().credentials();
        ServerRepository repository = MineCloud.instance().mongo().repositoryBy(Server.class);
        Server server = new Server();

        server.setType(type);
        server.setNumber(repository.nextNumberFor(type));
        server.setNetwork(network);
        server.setNode(MineCloudDaemon.instance().node());
        server.setOnlinePlayers(new ArrayList<>());
        server.setRamUsage(-1);
        server.setId(server.type().name() + server.number());
        server.setMetadata(metadata);
        server.setPort(PORT_COUNTER.incrementAndGet());
        server.setContainerId("null");

        Map<String, String> env = new HashMap<String, String>() {{
            put("mongo_hosts", mongoCreds.formattedHosts());
            put("mongo_username", mongoCreds.username());
            put("mongo_password", new String(mongoCreds.password()));
            put("mongo_database", mongoCreds.database());

            put("redis_host", redisCreds.hosts()[0]);
            put("redis_password", new String(redisCreds.password()));
            put("SERVER_MOD", server.type().mod());
            put("DEDICATED_RAM", String.valueOf(server.type().dedicatedRam()));
            put("MAX_PLAYERS", String.valueOf(server.type().maxPlayers()));

            put("server_id", server.entityId());
            put("DEFAULT_WORLD", type.defaultWorld().name());
            put("DEFAULT_WORLD_VERSION", type.defaultWorld().version());

            put("PORT", String.valueOf(server.port()));
            put("PRIVATE_IP", server.node().privateIp());
        }};

        startApplication(processScript("/mnt/minecloud/server/bukkit/" + server.type().mod() + "/init.sh", env), server.name());
        repository.save(server);
        MineCloud.logger().info("Started server " + server.name() + " with container id " + server.containerId());
    }

    public static void deployBungee(Network network, BungeeType type) {
        BungeeRepository repository = MineCloud.instance().mongo().repositoryBy(Bungee.class);
        Node node = MineCloudDaemon.instance().node();
        Bungee bungee = new Bungee();

        if (repository.count("_id", node.publicIp()) > 0) {
            MineCloud.logger().log(Level.WARNING, "Did not create bungee on this node; public ip is already in use");
            return;
        }

        bungee.setId(node.publicIp());
        bungee.setType(type);

        Credentials mongoCreds = MineCloud.instance().mongo().credentials();
        Credentials redisCreds = MineCloud.instance().redis().credentials();
        Map<String, String> env = new HashMap<String, String>() {{
            put("mongo_hosts", mongoCreds.formattedHosts());
            put("mongo_username", mongoCreds.username());
            put("mongo_password", new String(mongoCreds.password()));
            put("mongo_database", mongoCreds.database());

            put("redis_host", redisCreds.hosts()[0]);
            put("redis_password", new String(redisCreds.password()));
            put("DEDICATED_RAM", String.valueOf(type.dedicatedRam()));

            put("bungee_id", node.publicIp());
        }};

        startApplication(processScript("/mnt/minecloud/scripts/bungee-init.sh", env), "bungee");

        bungee.setNetwork(network);
        bungee.setNode(node);
        bungee.setPublicIp(node.publicIp());

        repository.save(bungee);
        MineCloud.logger().info("Started bungee " + bungee.name() + " with id " + bungee.containerId());
    }

    public static int pidOf(String app) throws IOException {
        return Integer.parseInt(Files.readAllLines(Paths.get("/var/run/" + app + "/app.pid")).get(0));
    }

    public static boolean isRunning(String app) throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec("ps -p " + pidOf(app));

        process.waitFor();
        return process.exitValue() == 0;
    }

    private static List<String> processScript(String file, Map<String, String> env) {
        List<String> script;

        try {
            script = Files.readAllLines(Paths.get(file));
        } catch (IOException ex) {
            throw new MineCloudException(ex);
        }

        script.replaceAll((s) -> {
            Container<String> container = new Container<>(s);

            env.forEach((find, replace) -> container.set(container.get().replace("]" + find, replace)));

            return container.get();
        });

        return script;
    }

    private static void startApplication(List<String> startScript, String name) {
        File runDir = new File("/var/run/" + name);

        if (runDir.exists()) {
            runDir.delete();
        }

        runDir.mkdirs();

        try {
            Files.write(Paths.get(runDir.getAbsolutePath(), "init.sh"), startScript);
            new File(runDir, "init.sh").setExecutable(true);

            Process process = new ProcessBuilder()
                    .directory(runDir)
                    .redirectErrorStream(true)
                    .command("/usr/bin/screen", "-dm", "-S", name, "sh", "init.sh")
                    .start();
        } catch (IOException ex) {
            throw new MineCloudException(ex);
        }
    }

    private static class Container<T> {
        private T value;

        public Container(T value) {
            this.value = value;
        }

        public Container() {
            this.value = null;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }
}
