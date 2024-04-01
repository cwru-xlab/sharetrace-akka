#!/bin/bash

for filename in $(cd ./app/src/main/resources && ls $1); do
  bin/run.sh $filename
done