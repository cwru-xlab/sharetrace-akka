#!/usr/bin/env bash

# Ref: https://stackoverflow.com/a/54688673

(trap '' HUP INT
  for filename in $(cd ./app/src/main/resources && ls | grep "$1"); do
    bin/run.sh "$filename" "$2"
  done
) > nohup-$(date +"%s%").out 2>&1 < /dev/null &