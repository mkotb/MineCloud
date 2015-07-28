#!/bin/bash

echo "------------------------------------------"
echo ""
echo "MineCloud Server Initialize Script"
echo ""
echo "------------------------------------------"

echo ""
echo ""

mkdir worlds
mkdir plugins

cp -r /mnt/minecloud/server/bukkit/$SERVER_MOD/* .
cp -r /mnt/minecloud/worlds/$DEFAULT_WORLD/$DEFAULT_WORLD_VERSION/* $DEFAULT_WORLD/
cp -r /mnt/minecloud/plugins/minecloud-bukkit/latest/* plugins/

sh start.sh
