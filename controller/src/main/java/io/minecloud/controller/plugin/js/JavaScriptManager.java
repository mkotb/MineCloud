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
package io.minecloud.controller.plugin.js;

import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.controller.plugin.PluginManager;
import org.apache.logging.log4j.Level;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JavaScriptManager implements PluginManager<JavaScriptPlugin> {
    private final Set<JavaScriptPlugin> plugins = new HashSet<>();
    private final ScriptEngine engine;

    private JavaScriptManager(File directory) {
        engine = new ScriptEngineManager().getEngineByName("JavaScript");

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Provided file is not a directory!");
        }

        File[] files = directory.listFiles();

        if (files == null)
            return;

        for (File file : files) {
            try {
                loadPlugin(file);
            } catch (MineCloudException e) {
                MineCloud.logger().log(Level.ERROR,
                        "Unable to load JavaScript plugin " + file.getName() + "!",
                        e);
            }
        }
    }

    public static JavaScriptManager createManager(File directory) {
        if (!directory.exists())
            directory.mkdirs();

        return new JavaScriptManager(directory);
    }

    @Override
    public Collection<JavaScriptPlugin> plugins() {
        return plugins;
    }

    @Override
    public JavaScriptPlugin loadPlugin(File file) {
        try {
            Reader reader = new FileReader(file);
            engine.eval(reader);

            JavaScriptPlugin plugin = new JavaScriptPlugin((Invocable) engine, file);

            plugins.add(plugin);
            return plugin;
        } catch (ScriptException | FileNotFoundException e) {
            throw new MineCloudException(e);
        }
    }
}
