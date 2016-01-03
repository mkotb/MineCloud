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

import com.mongodb.*;
import io.minecloud.db.mongo.model.MongoEntity;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;

public abstract class AbstractMongoRepository<T extends MongoEntity> extends BasicDAO<T, String> implements MongoRepository<T> {
    protected DBCollection collection;

    protected AbstractMongoRepository(Class<T> entity, Datastore datastore) {
        super(entity, datastore);
        collection = getCollection();
    }

    @Override
    public String collectionName() {
        return collection.getName();
    }

    @Override
    public T findFirst(String id) {
        return findOne("_id", id);
    }

    @Override
    public DBCollection collection() {
        return this.getCollection();
    }

    @Override
    public Collection<T> models() {
        return find(createQuery().disableValidation()).asList();
    }

    @SuppressWarnings("unchecked")
    public Class<T> modelClass() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
