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
package io.minecloud.models.nodes;

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;

public class CPU implements MongoModel {
    @DataField
    private double baseFrequency;
    @DataField
    private double maxFrequency;
    @DataField
    private int cores;
    @DataField
    private int threads;

    public double baseFrequency() {
        return baseFrequency;
    }

    public double maxFrequency() {
        return maxFrequency;
    }

    public int cores() {
        return cores;
    }

    public int threads() {
        return threads;
    }
}
