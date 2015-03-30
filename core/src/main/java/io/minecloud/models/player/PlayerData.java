package io.minecloud.models.player;

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;
import lombok.Setter;

import java.util.List;

public class PlayerData implements MongoModel {
    @DataField(name = "uuid")
    @Setter
    private String id;
    @DataField
    @Setter
    private String name;

    @DataField
    @Setter
    private double health;

    @DataField
    @Setter
    private double maxHealth;

    @DataField
    private List<PlayerMetadata> metadata;

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public double health() {
        return health;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public List<PlayerMetadata> metadata() {
        return metadata;
    }
}
