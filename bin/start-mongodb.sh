#!/usr/bin/env sh

# Ref: https://hub.docker.com/_/mongo/
docker pull mongo --quiet
docker run --name mongodb -p 27017:27017 -d mongo:latest