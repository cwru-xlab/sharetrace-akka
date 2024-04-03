#!/usr/bin/env bash

for filename in $(cd ./app/src/main/resources && ls | grep "$1"); do
  bin/run.sh "$filename" "$2"
done