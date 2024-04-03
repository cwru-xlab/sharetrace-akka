#!/usr/bin/env bash

trap "exit" INT

for network in "barabasi-albert" "gnm-random" "random-regular" "scale-free" "watts-strogatz"; do
  # Ref: https://stackoverflow.com/a/54688673
  nohup bin/run-all.sh "send-coefficient_${network}" > nohup-$(date +"%s").out 2>&1 < /dev/null &
  sleep 1
done