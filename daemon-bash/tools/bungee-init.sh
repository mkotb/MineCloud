#!/bin/bash

echo "------------------------------------------"
echo ""
echo "MineCloud Bungee Initialize Script"
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
DEDICATED_RAM=]DEDICATED_RAM
bungee_id=]bungee_id

mkdir plugins

cp -r /mnt/minecloud/server/bungee/* .
cp -r /mnt/minecloud/plugins/minecloud-bungee/latest/* plugins/

{ java -jar bungee.jar <&3 3<&- & } 3<&0
echo $! > app.pid