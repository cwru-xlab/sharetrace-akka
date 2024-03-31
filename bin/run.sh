#!/bin/bash

echo "RUN STARTED"

cd app

logsDirectory=logs/$(date +%s)
configFile=$1

echo "Config file: $configFile"
echo "Logs directory: $logsDirectory"

echo "Running..."
../gradlew :app:run -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

echo "Analyzing..."
../gradlew :app:analyze -Dconfig.resource=$configFile -Dlogs.dir=$logsDirectory

echo "Cleaning up event logs..."
rm -f $logsDirectory/event*.log*

echo "RUN COMPLETE"