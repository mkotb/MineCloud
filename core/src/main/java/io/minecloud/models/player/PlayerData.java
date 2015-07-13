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
package io.minecloud.models.player;

import lombok.EqualsAndHashCode;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode
public class PlayerData {
    @Setter
    private String id;
    @Setter
    private String name;
    @Setter
    private double health;
    @Setter
    private double maxHealth;
    @Setter
    private List<PlayerMetadata> metadata;

    public String uuid() {
        return id;
    }

    public String name() {
        return name;
    }

    public double health() {
        return health;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public List<PlayerMetadata> metadata() {
        if (metadata == null) {
            metadata = new ArrayList<>();
        }

        return metadata;
    }

    public void addMetadata(PlayerMetadata data) {
        metadata().removeIf((md) -> md.key().equals(data.key())); // override old entries
        metadata().add(data);
    }

    public Optional<PlayerMetadata> metadataBy(String name) {
        return metadata().stream()
                .filter((md) -> md.key().equals(name))
                .findFirst();
    }

    public boolean hasMetadata(String name) {
        return metadataBy(name).isPresent();
    }
}
