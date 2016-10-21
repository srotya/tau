#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: Run script for U
#

envsubst < /opt/tau/template.properties > /opt/tau/config.properties

export tauConfig=/opt/tau/config.properties

/usr/local/tomcat/bin/startup.sh

tail -f /usr/local/tomcat/logs/catalina.out