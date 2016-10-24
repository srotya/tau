#!/bin/bash

set -eu

export MYSQL_ROOT_PASSWORD=root
export DOCKER_REGISTRY=localhost:5000

# Stop any existing revisions of the containers
docker-compose stop -t 2

# Remove any existing revisions of the containers
docker-compose rm --force

export HVERSION=`cd ../..;mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`

echo $HVERSION

docker-compose up