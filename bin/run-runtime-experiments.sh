#!/usr/bin/env bash

trap "exit" INT

bin/make-configs.py runtime

for network in "barabasi-albert" "gnm-random" "random-regular" "watts-strogatz"; do
  # Ref: https://stackoverflow.com/a/54688673
  nohup bin/run-all.sh "runtime_${network}" > nohup-$(bin/current-time.sh).out 2>&1 < /dev/null &
  sleep 1
done