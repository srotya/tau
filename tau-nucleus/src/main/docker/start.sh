#!/bin/bash
#
# Author: Ambud Sharma
#
# Purpose: Setup and start the Nucleus App
############################################

# To create a tmpfs / ramfs for RocksDB Memstore
# mkdir /opt/walmap
# mount -t tmpfs -o size=512M tmpfs /opt/walmap

while ! nc -z $API_HOST $API_PORT;do echo "Checking API connectivity";sleep 1;done

envsubst < /opt/tau/template.conf > /opt/tau/nucleus.conf

java -Xms2G -Xmx2G -jar /opt/tau/nucleus.jar server /opt/tau/nucleus.yml 