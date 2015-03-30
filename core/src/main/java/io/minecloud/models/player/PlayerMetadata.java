package io.minecloud.models.player;

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;

public class PlayerMetadata implements MongoModel {
    @DataField
    private String value;

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
