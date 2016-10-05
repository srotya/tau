#!/bin/bash
#
# Author: Ambud Sharma
# Purpose: To facilitate run docker ui
#

ui=$(docker ps -q -f "name=hendrix")
db=$(docker ps -q -f "name=mysql")

if [ ! -z "$ui" ]; then
	docker stop $ui
	docker rm $ui
fi

if [ ! -z "$db" ]; then
	docker stop $db
	docker rm $db
fi

ip=`docker-machine ip`
docker run -d -p 32770:3306 -e MYSQL_ROOT_PASSWORD=root --name mysql mysql:latest
res=`pwd`
echo "Config location $res mounted"

mkdir -p ~/hendrix
cp $res/template.properties ~/hendrix/config.properties
sed -i '' 's/saddr/'$ip'/g' ~/hendrix/config.properties

cd ~/hendrix
res=`pwd`
docker run -d -p 9000:8080 -e hendrixConfig='/opt/config.properties' -v $res:/opt/ -v $res:/tmp/ --name hendrix hendrix
