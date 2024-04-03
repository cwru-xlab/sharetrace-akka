#!/usr/bin/env bash

for network in "barabasi-albert" "gnm-random" "random-regular" "scale-free" "watts-strogatz"; do
  bin/run-all.sh "send-coefficient_${network}*.conf"
  sleep 1
done