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
package io.minecloud.cli;

import asg.cliche.ShellFactory;
import io.minecloud.MineCloud;
import io.minecloud.cli.handler.MainHandler;
import io.minecloud.db.Credentials;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public final class MineCloudCLI {

    private MineCloudCLI() throws Exception {
        MainHandler main = new MainHandler();

        main.setCurrent(ShellFactory.createConsoleShell("minecloud", "MineCloud CLI", main));
        main.currentShell().commandLoop();
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        File configFolder = new File("/etc/minecloud/controller/");
        File file = new File(configFolder, "details.properties");

        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        properties.load(new FileInputStream(file));

        if (!properties.containsKey("mongo-hosts")) {
            MineCloud.runSetup(properties, file);
            new MineCloudCLI();
        }

        Credentials mongo = new Credentials(properties.getProperty("mongo-hosts").split(";"),
                properties.getProperty("mongo-username"),
                properties.getProperty("mongo-password").toCharArray(),
                properties.getProperty("mongo-database"));

        MineCloud.instance().initiateMongo(mongo);

        new MineCloudCLI();
    }
}
