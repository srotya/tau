#!/bin/bash
#
# Author: Ambud Sharma
# 
# Purpose: To build and install hendrix and run it on docker
#
set -eu

if [ -z $JAVA_HOME ]; then
	echo "JAVA_HOME not set"
	exit
fi

export MYSQL_ROOT_PASSWORD=root

# Cleanup all orphan untagged images
# docker rmi $(docker images | grep "^<none>" | awk "{print $3}")

# Stop any existing revisions of the containers
docker-compose -f ../conf/local/docker-compose-backup.yml stop -t 2

# Remove any existing revisions of the containers
docker-compose -f ../conf/local/docker-compose-backup.yml rm --force

# docker ps -a | grep 'registry' | awk '{print $1}' | xargs docker rm -f

# docker run -d -p 5000:5000 --restart=always --name registry registry:2 
export DOCKER_REGISTRY=localhost:5000

cd ../..
# Build the code and copy artifacts
# mvn -DskipTests clean package

export HVERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`

echo "HVERSION is $HVERSION"

cd install/scripts
docker-compose -f ../conf/local/docker-compose-backup.yml up -d

# Download Apache Storm to launch topologies
# wget http://apache.cs.utah.edu/storm/apache-storm-0.10.1/apache-storm-0.10.1.tar.gz -O /tmp/storm.tar.gz
# wget http://apache.cs.utah.edu/kafka/0.8.2.1/kafka_2.9.1-0.8.2.1.tgz -O /tmp/kafka.tar.gz

mkdir -p ../target/
cd ../target
rm -rf storm
rm -rf kafka
cp /tmp/storm.tar.gz .
cp /tmp/kafka.tar.gz .
tar xf storm.tar.gz
tar xf kafka.tar.gz
mv apache-storm-0.10.1 storm
mv kafka_2.9.1-0.8.2.1 kafka

docker-compose -f ../conf/local/docker-compose-backup.yml scale kafka=5
docker-compose -f ../conf/local/docker-compose-backup.yml scale supervisor=4

while ! nc -z localhost 49627;do echo "Checking nimbus availability";sleep 1;done

while ! `./storm/bin/storm list -c nimbus.thrift.port=49627 2>&1 | grep -q "No topologies"`;do echo "Waiting for Storm Nimbus to come online";sleep 1s; done || echo "Now deploying topology"

kafka/bin/kafka-topics.sh --describe --zookeeper localhost:2181 
kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --topic ruleTopic --replication-factor 3 --partitions 1
kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --topic templateTopic --replication-factor 3 --partitions 1

kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --topic logTopic --replication-factor 2 --partitions 5
kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --topic metricTopic --replication-factor 2 --partitions 5

./storm/bin/storm jar -c nimbus.host=localhost -c nimbus.thrift.port=49627 ../../hendrix-storm/target/hendrix-storm-$HVERSION-jar-with-dependencies.jar org.apache.storm.flux.Flux --remote ../conf/remote/rules.yml --filter ../conf/remote/config.properties

../scripts/create-tenant-template-rules.sh 

OS=`uname -s`
if [[ $OS=="Darwin" ]]; then
	open http://localhost:8084 
	open http://localhost:8080
fi