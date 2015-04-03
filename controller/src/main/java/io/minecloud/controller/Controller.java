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
package io.minecloud.controller;

import io.minecloud.MineCloud;
import io.minecloud.controller.plugin.PluginManager;
import io.minecloud.controller.plugin.js.JavaScriptManager;
import io.minecloud.controller.plugin.js.JavaScriptPlugin;
import io.minecloud.db.mongo.MongoDatabase;
import io.minecloud.db.redis.RedisDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Controller {
    private static Controller instance;

    private final RedisDatabase redis;
    private final MongoDatabase mongo;
    private final PluginManager<JavaScriptPlugin> jsManager;

    private Controller() {
        File scripts = new File("/etc/minecloud/controller/plugins/js");

        // TODO load parent JS file

        instance = this;

        this.redis = MineCloud.instance().redis();
        this.mongo = MineCloud.instance().mongo();
        this.jsManager = JavaScriptManager.createManager(scripts);
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        File configFolder = new File("/etc/minecloud/");
        File file = new File(configFolder, "controller/details.properties");

        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        properties.load(new FileInputStream(file));

        if (!properties.containsKey("mongo-hosts")) {
            MineCloud.runSetup(properties, file);
            new Controller();
        }

        new Controller();
    }

    public static Controller instance() {
        return instance;
    }
}
