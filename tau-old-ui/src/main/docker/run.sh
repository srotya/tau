#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: Run script for U
#

envsubst < /opt/hendrix/template.properties > /opt/hendrix/config.properties

export hendrixConfig=/opt/hendrix/config.properties

/usr/local/tomcat/bin/startup.sh

tail -f /usr/local/tomcat/logs/catalina.out