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
package io.minecloud.db.mongo.model;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;

public interface MongoModel {
    /**
     * Called after all data fields have been loaded successfully, may or may not have any significant effect.
     */
    public default void initialize() {}

    /**
     * Called after field-encoding for translation of model -> db object.
     * Used for any special encoding needs for the model.
     *
     * @param object Object to encode
     */
    public default void translate(BasicDBObject object) {}

    public default ObjectId objectId() {
        return ModelWrapper.wrapperFrom(this).objectId();
    }

}
