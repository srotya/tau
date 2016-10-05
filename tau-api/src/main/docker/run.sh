#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: Run script for API
#

#export MYSQL_ADDRESS=`getent hosts $MYSQL_ADDRESS | awk '{ print $1 }'`

while ! nc -z $MYSQL_HOST $MYSQL_PORT;do echo "Checking mysql connectivity";sleep 1;done

while ! nc -z $KAFKA_HOST $KAFKA_PORT;do echo "Checking kafka connectivity";sleep 1;done

envsubst < /opt/hendrix/template.properties > /opt/hendrix/config.properties

export hendrixConfig=/opt/hendrix/config.properties

java -jar /usr/local/hendrix/api.jar server /opt/hendrix/config.yaml
#tail -f /var/log/*