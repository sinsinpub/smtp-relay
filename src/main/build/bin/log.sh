#!/bin/bash

log_file=${1:-"all.log"}

tail -55f "log/$log_file"
