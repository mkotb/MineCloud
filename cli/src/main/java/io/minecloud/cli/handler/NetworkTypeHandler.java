package io.minecloud.cli.handler;

import asg.cliche.Command;
import io.minecloud.MineCloud;
import io.minecloud.models.bungee.Bungee;
import io.minecloud.models.bungee.type.BungeeType;
import io.minecloud.models.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Programmed by DevRo on 27/05/15.
 */
public class NetworkTypeHandler extends AbstractHandler {
    Network type;

    NetworkTypeHandler(String name) {
        type = MineCloud.instance().mongo()
                .repositoryBy(Network.class)
                .findFirst(name);

        if (type == null) {
            System.out.println("Could not find type in database; creating a new one...");
            type = new Network();

            type.setName(name);
        }
    }

    @Command
    public String addBungee(String bungeeName) {
        BungeeType bungeeType = MineCloud.instance().mongo()
                .repositoryBy(BungeeType.class)
                .findFirst(bungeeName);

        if (bungeeType == null) {
            return "No bungees found by the name of " + bungeeName;
        }

        if (type.bungeeMetadata() == null) {
            type.setBungees(new ArrayList<>());
        }

        List<BungeeType> bungeeTypes = type.bungeeMetadata();

        bungeeTypes.add(bungeeType);
        type.setBungees(bungeeTypes);

        return "Successfully added " + bungeeName + " to Network";
    }

}
