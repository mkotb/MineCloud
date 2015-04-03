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
import io.minecloud.controller.plugin.Plugin;
import org.apache.logging.log4j.Level;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.File;

public class JavaScriptPlugin implements Plugin {
    private final Invocable invocable;
    private final String name;
    private final File script;

    JavaScriptPlugin(Invocable invocable, File file) {
        this.invocable = invocable;
        this.name = file.getName().replace(".js", "");
        this.script = file;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return "1.0";
    }

    public File script() {
        return script;
    }

    public Object invokeFunction(String name, Object... args) {
        Object response;

        try {
            response = invocable.invokeFunction(name, args);
        } catch (NoSuchMethodException | ScriptException ex) {
            MineCloud.logger().log(Level.ERROR, "Unable to invoke function " + name, ex);
            return null;
        }

        return response;
    }
}
