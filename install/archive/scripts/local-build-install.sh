#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: To build and install Hendrix locally
#

if [ -z $JAVA_HOME ]; then
	echo "JAVA_HOME not set"
	exit
fi

# Create directories
mkdir -p ~/hendrix
mkdir -p ~/hendrix/log-data
mkdir -p ~/hendrix/alert-data

cd ../..
# Build the code and copy artifacts
mvn clean package
# Copy artifacts
cd install/target
cp ../../hendrix-api/target/api.jar .
cp ../../hendrix-alerts/target/*-jar-with-dependencies.jar .
cp ../../hendrix-storm/target/*-jar-with-dependencies.jar .
cp ../../hendrix-ui/target/ROOT.war .

# Download Apache Tomcat 8 and install UI war into it
wget http://www.gtlib.gatech.edu/pub/apache/tomcat/tomcat-8/v8.0.35/bin/apache-tomcat-8.0.35.tar.gz
tar xf apache-tomcat-8.0.35.tar.gz
mv apache-tomcat-8.0.35 tomcat
rm -rf tomcat/webapps/*
cp ROOT.war tomcat/webapps/
tar czf tomcat.tgz tomcat

# Download Apache Storm
wget http://apache.cs.utah.edu/storm/apache-storm-0.10.1/apache-storm-0.10.1.tar.gz
tar xf apache-storm-0.10.1.tar.gz
mv apache-storm-0.10.1 storm