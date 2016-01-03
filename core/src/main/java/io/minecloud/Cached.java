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
package io.minecloud;

public final class Cached<T> {
    private final long threshold;
    private Function<T> function;
    private T value;
    private long lastSet;

    private Cached(long threshold, Function<T> function) {
        this.function = function;
        this.threshold = threshold;
        update();
    }

    public static <T> Cached<T> create(long threshold, Function<T> function) {
        return new Cached<>(threshold, function);
    }

    public static <T> Cached<T> create(Function<T> function) {
        return create(10000L, function);
    }

    public T get() {
        if ((System.currentTimeMillis() - lastSet) >= threshold) {
            update();
        }

        return value;
    }

    private void update() {
        this.value = function.get();
        this.lastSet = System.currentTimeMillis();
    }

    public interface Function<T> {
        T get();
    }
}
