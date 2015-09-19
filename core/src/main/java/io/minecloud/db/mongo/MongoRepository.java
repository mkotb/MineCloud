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
package io.minecloud.db.mongo;

import com.mongodb.DBCollection;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * MongoRepository is a means of data structure for MineCloud, this uses type T as form of a schema for the collection.
 * This being so, any invalid inputs or raw entries will immediately be either removed or ignored during load-time.
 * Using such a design keeps invalid data entries to be put into the database to cause destruction across the network,
 * reducing volatility of the software.
 * </p>
 *
 * @param <T>
 */
public interface MongoRepository<T> {
    public String collectionName();

    public T findFirst(String id);

    public default Collection<T> findAll(Predicate<T> predicate) {
        return models().stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Retrieves all models or entries in the repository
     * @return All models in the repository
     */
    public Collection<T> models();

    public DBCollection collection();
}
