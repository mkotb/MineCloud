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
import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * ModelTranslator is used to transfer Java "serialized" models into BSON to be inserted into MongoDB.
 * TODO
 * </p>
 */
public final class ModelTranslator {
    private static final Map<Class<?>, DataFieldConverter<?>> CONVERTERS = new HashMap<>();

    static {
        addConverter(String.class, Object::toString);
        addConverter(MongoModel.class, (obj) -> translate((BasicDBObject) obj,
                classBy(((BasicDBObject) obj).getString("modelClass"))).model());
        addConverter(int.class, (obj) -> (int) obj);
        addConverter(double.class, (obj) -> (double) obj);
        addConverter(Date.class, (obj) -> (Date) obj);
    }

    private static Class classBy(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            throw new MineCloudException(ex);
        }
    }

    private ModelTranslator() {}

    public static <T extends MongoModel> ModelWrapper translate(BasicDBObject object, Class<T> cls) {
        if (!object.containsField("timeCreated") || object.containsField("lastUpdated")) {
            return null; // don't translate
        }

        if (!cls.getName().equals(cls.getName()) || !MongoModel.class.isAssignableFrom(cls)) {
            return null;
        }

        T model;

        try {
            cls.getConstructor();
        } catch (NoSuchMethodException ex) {
            MineCloud.logger().warn("There was no default constructor for model class " + cls.getSimpleName());
            return null;
        }

        try {
            model = cls.newInstance();
        } catch (Exception ex) {
            MineCloud.logger().log(Level.ERROR, "Unable to create new instance of " + cls.getName() + "...", ex);
            return null;
        }

        for (Field field : cls.getDeclaredFields()) {
            if (!field.isAnnotationPresent(DataField.class)) {
                continue;
            }

            DataField data = field.getAnnotation(DataField.class);
            String name = data.name();

            if ("filler_In".equals(name)) {
                name = field.getName();
            }

            if (!data.optional() && !object.containsField(data.name())) {
                return null; // invalid object C:
            } else if (data.optional() && !object.containsField(data.name())) {
                continue;
            }

            if (!CONVERTERS.containsKey(field.getType()) && data.optional()) {
                continue;
            } else if (!CONVERTERS.containsKey(field.getType()) && !data.optional()) {
                throw new MineCloudException("There is no data converter for type " +
                        field.getType().getSimpleName() + " for non-optional field " +
                        field.getName() + " in " + cls.getSimpleName());
            }

            if (Modifier.isFinal(field.getModifiers()) && !data.optional()) {
                throw new MineCloudException("Cannot set a final field on non-optional field " +
                        field.getName() + " in " + cls.getSimpleName());
            } else if (data.optional() && Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            if (Modifier.isStatic(field.getModifiers())) {
                MineCloud.logger().warn("Found static data field for an instance-based model: " +
                        field.toString() + ". Setting anyways...");
            }

            try {
                if (Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                    field.setAccessible(true);
                }

                field.set(model, CONVERTERS.get(field.getClass()).convert(object.get(name)));
            } catch (IllegalAccessException ignored) {}
        }

        model.initialize();
        return ModelWrapper.create(object.getDate("timeCreated"), object.getDate("lastUpdated"), model,
                object.getObjectId("_id"));
    }

    public static BasicDBObject translate(ModelWrapper wrapper) {
        BasicDBObject object = new BasicDBObject();
        MongoModel model = wrapper.model();

        object.append("timeCreated", wrapper.timeSerialized());
        object.append("lastUpdated", Date.from(Instant.now()));

        for (Field field : model.getClass().getDeclaredFields()) {
            if(!field.isAnnotationPresent(DataField.class)) {
                continue;
            }

            DataField data = field.getAnnotation(DataField.class);
            String name = data.name();
            Object value;

            if("filler_In".equals(name)) {
                name = field.getName();
            }

            try {
                if (Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                    field.setAccessible(true);
                }

                value = field.get(model);
            } catch (IllegalAccessException ignored) {
                value = null;
            }

            if (value == null && !data.optional()) {
                throw new MineCloudException("Value cannot be null for non-optional data field " +
                        data.name());
            } else if (data.optional() && value == null) {
                continue;
            }

            if (Modifier.isStatic(field.getModifiers())) {
                MineCloud.logger().warn("Found static data field for an instance-based model: " +
                        field.toString() + ". Encoding anyways...");
            }

            object.append(name, value); // FIXME: only supports strings, primitives, and Dates
        }

        model.translate(object);
        return object;
    }

    public static <T> void addConverter(Class<T> cls, DataFieldConverter<T> converter) {
        CONVERTERS.put(cls, converter);
    }
}
