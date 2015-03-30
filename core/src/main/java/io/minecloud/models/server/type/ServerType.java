package io.minecloud.models.server.type;

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;
import lombok.Setter;

public class ServerType implements MongoModel {
    @DataField
    @Setter
    private String name;
    @DataField
    @Setter
    private int minimumAmount;
    @DataField
    @Setter
    private int maximumAmount;
    @DataField
    @Setter
    private int maxPlayers;
    @DataField
    @Setter
    private String preferredNode;

    public String name() {
        return name;
    }

    public int minimumAmount() {
        return minimumAmount;
    }

    public int maximumAmount() {
        return maximumAmount;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public String preferredNode() {
        return preferredNode;
    }
}
