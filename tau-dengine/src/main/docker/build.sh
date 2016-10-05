#!/bin/bash
#
# Author: Ambud Sharma
# Purpose: To facilitate docker builds for local Storm
#

docker build --no-cache --rm=true -t hendrixstorm .
