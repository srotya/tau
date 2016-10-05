#!/bin/bash
############################################
#
# Author: Ambud Sharma
#
############################################

source ./hendrix-env.sh

set -eu

# Create required kafka topics
$KAFKA_HOME/bin/kafka-topics.sh --create --topic logTopic --zookeeper $ZK_SERVER --partitions $PARTITION_COUNT --replication-factor $REPLICATION_FACTOR
$KAFKA_HOME/bin/kafka-topics.sh --create --topic metricTopic --zookeeper $ZK_SERVER --partitions $PARTITION_COUNT --replication-factor $REPLICATION_FACTOR
$KAFKA_HOME/bin/kafka-topics.sh --create --topic alertTopic --zookeeper $ZK_SERVER --partitions 1 --replication-factor 2
$KAFKA_HOME/bin/kafka-topics.sh --create --topic ruleTopic --zookeeper $ZK_SERVER --partitions 1 --replication-factor 2
$KAFKA_HOME/bin/kafka-topics.sh --create --topic templateTopic --zookeeper $ZK_SERVER --partitions 1 --replication-factor 2

echo "Topics created!"