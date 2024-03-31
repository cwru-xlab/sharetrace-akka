#!/bin/sh

# Increase data rate limit for viewing progress when analyzing event logs
jupyter lab --ServerApp.iopub_data_rate_limit=1000000000
