package io.minecloud.models.server.type;

import io.minecloud.db.mongo.AbstractMongoRepository;
import io.minecloud.db.mongo.MongoDatabase;

public class ServerTypeRepository extends AbstractMongoRepository<ServerType> {
    private ServerTypeRepository(MongoDatabase database) {
        super("server-types", database);
    }

    public ServerType serverTypeBy(String name) {
        return findFirst((type) -> type.name().equalsIgnoreCase(name));
    }
}
