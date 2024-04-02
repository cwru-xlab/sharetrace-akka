#!/bin/bash

trap "exit" INT

for filename in $(cd ./app/src/main/resources && ls $1); do
  bin/run.sh $filename
done