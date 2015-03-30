package io.minecloud.models.server;

import com.mongodb.DBObject;
import io.minecloud.db.mongo.AbstractMongoRepository;
import io.minecloud.db.mongo.MongoDatabase;

public class ServerRepository extends AbstractMongoRepository<Server> {

    private ServerRepository(MongoDatabase database) {
        super("servers", database);
    }

    public static ServerRepository create(MongoDatabase database) {
        return new ServerRepository(database);
    }

    @Override
    public boolean update(DBObject query, Server model) {
        throw new UnsupportedOperationException();
    }
}
