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

import io.minecloud.MineCloud;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.nodes.CoreMetadata;
import io.minecloud.models.nodes.Node;
import io.minecloud.models.server.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.IntStream;

// I'm sorry for this class, I recommend almost nobody reads this

/**
 * A very bad hack to watch statistics on the node
 */
public class StatisticsWatcher extends Thread {
    private int[] prevTotal;
    private int[] prevIdle;

    @Override
    public void run() {
        while(!isInterrupted()) {
            Node node = MineCloudDaemon.instance().node();
            double[] frequencies = new double[node.type().processor().threads()];
            double[] usages = new double[frequencies.length];
            int index = -1;

            for (File file : new File("/sys/devices/system/cpu/").listFiles((f, s) -> s.startsWith("cpu"))) {
                if (!file.isDirectory())
                    continue;

                File currentFreq = new File(file, "cpufreq/scaling_cur_freq");

                if (!currentFreq.exists())
                    continue;

                int frequency;

                try {
                    frequency = Integer.parseInt(Files.readAllLines(currentFreq.toPath()).get(0));
                } catch (IOException ex) {
                    MineCloud.logger().log(Level.SEVERE, "Was unable to fetch CPU frequency", ex);
                    continue;
                }

                frequencies[++index] = ((double) frequency / 1000000);
            }

            if (prevTotal == null) {
                prevTotal = new int[frequencies.length];
                prevIdle = new int[frequencies.length];
            }

            List<String> stat;

            try {
                stat = Files.readAllLines(Paths.get("/proc/stat"));
            } catch (IOException ex) {
                MineCloud.logger().log(Level.SEVERE, "Was unable to fetch CPU statistics", ex);
                continue;
            }

            IntStream.range(0, usages.length).forEach((i) -> {
                String prefix = "cpu" + i;
                String cpuStat = stat.stream()
                        .filter((s) -> s.startsWith(prefix))
                        .findFirst()
                        .get();

                cpuStat = cpuStat.substring(4); // redact prefix "cpun "
                String[] statistics = cpuStat.split(" ");

                int idle = Integer.parseInt(statistics[3]);
                int totalCpuTime = 0;

                for (String s : statistics) {
                    if ("".equals(s) || s == null)
                        continue;

                    totalCpuTime += Integer.parseInt(s);
                }

                int diffIdle = idle - prevIdle[i];
                int diffTotal = totalCpuTime - prevTotal[i];

                usages[i] = (1000 * (diffTotal - diffIdle) / diffTotal + 5) / 10;
                prevIdle[i] = idle;
                prevTotal[i] = totalCpuTime;
            });

            Collection<Server> servers = MineCloud.instance().mongo()
                    .repositoryBy(Server.class)
                    .findAll((server) -> server.node().name().equals(node.name()));
            Collection<Bungee> bungees = MineCloud.instance().mongo()
                    .repositoryBy(Bungee.class)
                    .findAll((bungee) -> bungee.node().name().equals(node.name()));
            int ramUsed = 0;

            for (Server server : servers) {
                ramUsed += server.ramUsage();
            }

            for (Bungee bungee : bungees) {
                ramUsed += bungee.ramUsage();
            }

            node.setAvailableRam(node.type().ram() - ramUsed);

            List<CoreMetadata> cores = new ArrayList<>();

            IntStream.range(0, usages.length).forEach((i) -> {
                CoreMetadata metadata = new CoreMetadata();

                metadata.setCurrentFrequency(frequencies[i]);
                metadata.setUsage(usages[i]);

                cores.add(metadata);
            });

            node.setCoreMetadata(cores);
            MineCloud.instance().mongo().repositoryBy(Node.class).save(node);

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                // I don't care
            }
        }
    }
}
