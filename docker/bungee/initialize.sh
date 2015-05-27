#!/bin/bash

echo "------------------------------------------"
echo ""
echo "MineCloud Bungee Initialize Script"
echo ""
echo "------------------------------------------"

echo ""
echo ""

mkdir plugins

cp -r /mnt/minecloud/server/bungee/* .
cp -r /mnt/minecloud/plugins/minecloud-bungee/latest/* plugins/

sh start.sh
