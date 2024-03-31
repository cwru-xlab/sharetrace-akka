#!/bin/bash

echo "RUN STARTED"

cd app

configFile="$1"
logsDirectory="logs/$(date +%s)"

echo "Config file: $configFile"
echo "Logs directory: $logsDirectory"

echo "Running..."
../gradlew :app:run -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

echo "Analyzing..."
../gradlew :app:analyze -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

if [ "$2" = "--no-clean-up" ]; then
  echo "Skipping event log cleanup"
else
  echo "Cleaning up event logs..."
  rm -f $logsDirectory/event*.log*
fi

echo "RUN COMPLETE"