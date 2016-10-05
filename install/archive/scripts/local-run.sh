#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: To run Hendrix locally
#
cd ../target

export HENDRIX_CONFIG=../conf/local/config.properties
nohup java -jar api.jar server ../conf/local/app.yaml &

tomcat/bin/startup.sh

export STORM_HOME=./storm
$STORM_HOME/bin/storm jar hendrix-storm-0.0.4-SNAPSHOT-jar-with-dependencies.jar org.apache.storm.flux.Flux --local ../conf/local/rules-local.yaml