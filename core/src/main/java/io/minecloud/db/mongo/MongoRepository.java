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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.minecloud.db.mongo.model.MongoModel;
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
public interface MongoRepository<T extends MongoModel> {

    public String collectionName();

    public default T findFirst(Predicate<T> predicate) {
        Optional<T> optional = models().stream()
                .filter(predicate)
                .findFirst();

        return optional.isPresent() ? optional.get() : null;
    }

    public default Collection<T> findAll(Predicate<T> predicate) {
        return models().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public T findFirst(BasicDBObject query);

    public T findFirst(ObjectId id);

    /**
     * Retrieves all models or entries in the repository
     * @return All models in the repository
     */
    public Collection<T> models();

    /**
     * Inserts a module into the repository
     * @param model Model you wish to insert
     * @return If module was able to be inserted successfully, will return false if model has invalid values.
     */
    public boolean insert(T model);

    /**
     * Removes a value from the repository based on predicate provided
     * @param predicate Predicate wished to be used when performing a search
     */
    public default void remove(Predicate<T> predicate) {
        models().stream()
                .filter(predicate)
                .forEach(this::remove);
    }

    public void remove(T model);

    public boolean update(ObjectId id, T model);

    public boolean update(DBObject query, T model);

    public void update(T model);

    public DBCollection collection();
}
