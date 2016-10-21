#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: Run script for API
#

#export MYSQL_ADDRESS=`getent hosts $MYSQL_ADDRESS | awk '{ print $1 }'`

while ! nc -z $MYSQL_HOST $MYSQL_PORT;do echo "Checking mysql connectivity";sleep 1;done

envsubst < /opt/tau/template.properties > /opt/tau/config.properties

java -jar /usr/local/tau/api.jar server /opt/tau/config.yaml
#tail -f /var/log/*