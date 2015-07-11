package io.minecloud.models.plugins;

import io.minecloud.db.mongo.AbstractMongoRepository;
import org.mongodb.morphia.Datastore;

public class PluginTypeRepository extends AbstractMongoRepository<PluginType> {
    private PluginTypeRepository(Datastore ds) {
        super(PluginType.class, ds);
    }

    public static PluginTypeRepository create(Datastore ds) {
        return new PluginTypeRepository(ds);
    }
}
