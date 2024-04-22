#!/usr/bin/env bash

echo "RUN STARTED"

if [[ "$OSTYPE" == "darwin"* ]]; then
  currentTime=$(gdate "+%s%3N")
else
  currentTime=$(date +"%s%3N")
fi

logsDirectory="logs/$currentTime"
configFile="$1"

echo "Config file: $configFile"
echo "Logs directory: $logsDirectory"

cd app

echo "Running..."
../gradlew run -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

echo "Analyzing..."
../gradlew analyze -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

if [[ "$2" == "--no-clean-up" ]]; then
  echo "Skipping event log cleanup"
else
  echo "Cleaning up event logs..."
  rm -f $logsDirectory/event*.log*
  echo "Renaming results file..."
  mv $logsDirectory/results*.json $logsDirectory/results.json
fi

echo "RUN COMPLETED"