#!/bin/bash

./stop.sh "$@"
if [ $? -gt 1 ]; then
  exit $?
fi
sleep 1
./start.sh "$@"
if [ $? -ne 0 ]; then
  exit $?
fi
