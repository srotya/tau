#!/bin/bash
#
# Author: Ambud Sharma
#
# Purpose: Setup and start the Nucleus App
############################################

# To create a tmpfs / ramfs for RocksDB Memstore
# mkdir /opt/walmap
# mount -t tmpfs -o size=512M tmpfs /opt/walmap

envsubst < /opt/tau/template.conf > /opt/tau/nucleus.conf

java -Xms2G -Xmx2G -jar /opt/tau/nucleus.jar server /opt/tau/nucleus.yml 