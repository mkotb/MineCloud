package io.minecloud.models.nodes.spec;

import io.minecloud.db.mongo.AbstractMongoRepository;
import io.minecloud.db.mongo.MongoDatabase;

public class NodeSpecificationRepository extends AbstractMongoRepository<NodeSpecification> {

    private NodeSpecificationRepository(MongoDatabase database) {
        super("node-specifications", database);
    }

    public static NodeSpecificationRepository create(MongoDatabase database) {
        return new NodeSpecificationRepository(database);
    }

    public NodeSpecification specificationBy(String name) {
        return findFirst((spec) -> spec.name().equalsIgnoreCase(name));
    }
}
