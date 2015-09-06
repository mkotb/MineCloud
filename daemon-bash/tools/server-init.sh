#!/bin/bash

echo "------------------------------------------"
echo ""
echo "MineCloud Server Initialize Script"
echo ""
echo "------------------------------------------"

echo ""
echo ""

mongo_hosts=]mongo_hosts
mongo_username=]mongo_username
mongo_password=]mongo_password
mongo_database=]mongo_database

redis_host=]redis_host
redis_password=]redis_password
SERVER_MOD=]SERVER_MOD
DEDICATED_RAM=]DEDICATED_RAM
MAX_PLAYERS=]MAX_PLAYERS
server_id=]server_id
DEFAULT_WORLD=]DEFAULT_WORLD
DEFAULT_WORLD_VERSION=]DEFAULT_WORLD_VERSION
PORT=]PORT
PRIVATE_IP=]PRIVATE_IP

mkdir worlds
mkdir plugins
mkdir worlds/$DEFAULT_WORLD

cp -r /mnt/minecloud/server/bukkit/$SERVER_MOD/* .
cp -r /mnt/minecloud/worlds/$DEFAULT_WORLD/$DEFAULT_WORLD_VERSION/* worlds/$DEFAULT_WORLD/
cp -r /mnt/minecloud/plugins/minecloud-bukkit/latest/* plugins/

sed -i "s/levelname/$DEFAULT_WORLD/" server.properties
sed -i "s/maxplayers/$MAX_PLAYERS/" server.properties
sed -i "s/port/$PORT" server.properties
sed -i "s/privateip/$PRIVATE_IP" server.properties

java -jar server.jar