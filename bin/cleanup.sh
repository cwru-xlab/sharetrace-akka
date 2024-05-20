#!/usr/bin/env sh

rm -f nohup*
rm -rf app/logs
(cd app/src/main/resources && ls | awk '!/template|base/' | xargs rm -f)