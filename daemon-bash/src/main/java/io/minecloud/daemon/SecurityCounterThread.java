package io.minecloud.daemon;

import io.minecloud.MineCloud;

import java.util.logging.Level;

/**
 * @author Erik Rosemberg
 * @since 29.12.2015
 */
public class SecurityCounterThread extends Thread {

    @Override
    public void run() {
        while (!isInterrupted()) {
            MineCloudDaemon.instance().counters.stream().filter(MineCloudDaemon.SecurityCounter::shouldKill).forEach(counter -> {
                MineCloudDaemon.instance().killServer(counter.getServer());
                MineCloud.logger().log(Level.SEVERE, "A crashed server was found but was killed after 2 checks. " + counter.getServer().entityId());
            });

            try {
                sleep(20_000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
