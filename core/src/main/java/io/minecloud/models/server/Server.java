package io.minecloud.models.server;

import io.minecloud.db.mongo.model.DataField;
import io.minecloud.db.mongo.model.MongoModel;
import io.minecloud.models.player.PlayerData;
import lombok.Setter;

import java.util.List;

/*
 * It is not recommended to play with the values of this class.
 *
 * Any inconsistent changes made to this class will be ignored.
 */
public class Server implements MongoModel {
    @DataField
    @Setter
    private String serverType;
    @DataField
    @Setter
    private String nodeId;
    @DataField
    @Setter
    private List<PlayerData> onlinePlayers;
    @DataField
    @Setter
    private int ramUsage;
    @DataField
    @Setter
    private int port;
    @DataField
    @Setter
    private int number;
    @DataField
    @Setter
    private String containerId;

    public String serverType() {
        return serverType;
    }

    public String nodeId() {
        return nodeId;
    }

    public List<PlayerData> onlinePlayers() {
        return onlinePlayers;
    }

    public int ramUsage() {
        return ramUsage;
    }

    public int port() {
        return port;
    }

    public int number() {
        return number;
    }

    public String containerId() {
        return containerId;
    }
}
