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

import org.bukkit.scheduler.BukkitRunnable;

public class TPSTracker extends BukkitRunnable {
    private static final int TICK_HISTORY = 600;

    private int tickCount;
    private long[] ticks = new long[TICK_HISTORY];

    @Override
    public void run() {
        ticks[tickCount] = System.currentTimeMillis();
        tickCount++;

        if (tickCount == TICK_HISTORY) {
            tickCount = 0;
        }
    }

    public double fetchTps() {
        return fetchTps(100);
    }

    public double fetchTps(int time) {
        int target = (tickCount - 1 - time) % ticks.length;
        long elapsed = System.currentTimeMillis() - ticks[Math.abs(target)];

        return time / (elapsed / 1000.0D);
    }
}
