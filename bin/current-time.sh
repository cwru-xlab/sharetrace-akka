#!/usr/bin/env sh

if [[ "$OSTYPE" == "darwin"* ]]; then
  gdate +"%s%3N"
else
  date +"%s%3N"
fi