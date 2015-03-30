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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.minecloud.MineCloud;
import io.minecloud.MineCloudException;
import io.minecloud.db.mongo.MongoRepository;
import org.apache.logging.log4j.Level;
import org.bson.types.ObjectId;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;

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
        addConverter(List.class, (obj) -> {
            BasicDBList raw = (BasicDBList) obj;
            List list = new ArrayList();

            for (Object o : raw) {
                list.add(getConverter(o.getClass()).convert(o));
            }

            return list;
        });
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
            return null; // don't translate, invalid object
        }

        String className = object.getString("modelClass");

        if (!className.equals(cls.getName())) {
            try {
                cls = classBy(className);
            } catch (MineCloudException ignored) {
            }
        }

        if (!MongoModel.class.isAssignableFrom(cls)) {
            throw new MineCloudException("Class provided is not assignable from MongoModel"); // in some world
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
            if (!field.isAnnotationPresent(DataField.class)) { // ignore fields which are not data fields
                continue;
            }

            DataFieldConverter<T> converter = (DataFieldConverter<T>) getConverter(field.getType());
            DataField data = field.getAnnotation(DataField.class);
            String name = data.name();

            if ("filler_In".equals(name)) {
                name = field.getName(); // replace name with field name if not overridden
            }

            if (!data.optional() && !object.containsField(data.name())) {
                return null; // invalid object C:
            } else if (data.optional() && !object.containsField(data.name())) {
                continue; // field is optional and object provided doesn't contain the field, so proceed without it
            }

            if (data.reference() && MongoModel.class.isAssignableFrom(field.getType())) {
                /*
                 * Find the according repository, and use the object id or name to find the referenced object
                 */
                MongoRepository repository = MineCloud.instance().mongo()
                        .repositoryBy(field.getType().asSubclass(MongoModel.class));
                MongoModel reference;
                Object identifier = object.get(data.name());

                if (identifier instanceof ObjectId) {
                    reference = repository.findFirst((ObjectId) identifier);
                } else if (identifier instanceof String) {
                    reference = repository.findFirst(new BasicDBObject("name", identifier));
                } else {
                    return null; // invalid identifier
                }

                converter = (obj) -> (T) reference;
            } else if (data.reference() && !MongoModel.class.isAssignableFrom(field.getType())) {
                throw new MineCloudException("Cannot get reference to non-model object" +
                        field.getType().getName() + " !");
            }

            if (converter == null && data.optional()) {
                continue;
            } else if (converter == null && !data.optional()) {
                throw new MineCloudException("There is no data converter for type " +
                        field.getType().getSimpleName() + " for non-optional field " +
                        field.getName() + " in " + cls.getSimpleName());
            }

            if (converter == null) {
                continue; // not possible, but to remove warnings
            }

            if (Modifier.isFinal(field.getModifiers()) && !data.optional()) {
                throw new MineCloudException("Cannot set a final field on non-optional field " +
                        field.getName() + " in " + cls.getSimpleName());
            } else if (data.optional() && Modifier.isFinal(field.getModifiers())) {
                continue; // field is optional and final at the same time, proceed anyways
            }

            if (Modifier.isStatic(field.getModifiers())) {
                MineCloud.logger().warn("Found static data field for an instance-based model: " +
                        field.toString() + ". Setting anyways...");
            }

            try {
                if (Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers())) {
                    field.setAccessible(true); // set accessible if needed
                }

                field.set(model, converter.convert(object.get(name)));
            } catch (IllegalAccessException ignored) {}
        }

        model.initialize(object);
        return ModelWrapper.create(object.getDate("timeCreated"), object.getDate("lastUpdated"), model,
                object.getObjectId("_id"));
    }

    public static BasicDBObject translate(ModelWrapper wrapper) {
        BasicDBObject object = new BasicDBObject();
        MongoModel model = wrapper.model();

        object.append("timeCreated", wrapper.timeSerialized());
        object.append("lastUpdated", Date.from(Instant.now()));
        object.append("modelClass", wrapper.model().getClass().getName());

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

            if (value == null) {
                continue; // not possible, but to remove warnings
            }

            if (MongoModel.class.isAssignableFrom(value.getClass())) {
                value = translate(ModelWrapper.wrapperFrom((MongoModel) value));

                if (data.reference()) {
                    BasicDBObject obj = (BasicDBObject) value;

                    if (obj.getString("name") != null) {
                        value = obj.getString("name");
                    } else {
                        value = obj.getObjectId("_id");
                    }
                }
            }

            if (List.class.isAssignableFrom(value.getClass())) {
                value = resolveList((List) value);
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

    private static BasicDBList resolveList(List list) {
        BasicDBList value = new BasicDBList();

        for (Object o : list) {
            if (MongoModel.class.isAssignableFrom(o.getClass())) {
                o = translate(ModelWrapper.wrapperFrom((MongoModel) o));
            }

            if (List.class.isAssignableFrom(o.getClass())) {
                o = resolveList((List) o);
            }

            value.add(o);
        }

        return value;
    }

    public static <T> DataFieldConverter<T> getConverter(Class<T> cls) {
        DataFieldConverter<T> converter = null;

        for (Class<?> clazz : CONVERTERS.keySet()) {
            if (clazz.equals(cls)) {
                return (DataFieldConverter<T>) CONVERTERS.get(clazz);
            }

            if (clazz.isAssignableFrom(cls)) {
                converter = (DataFieldConverter<T>) CONVERTERS.get(clazz);
            }
        }

        return converter;
    }

    public static <T> void addConverter(Class<T> cls, DataFieldConverter<T> converter) {
        CONVERTERS.put(cls, converter);
    }
}
