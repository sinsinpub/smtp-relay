#!/bin/bash

. "etc/env.conf"

# Check if server stops already
DAEMON_PID=`$PS_CMD | awk '/java .*-jar '$DAEMON_JAR'( '$DAEMON_ARGS')?$/{print$2}'`
if [ -z "$DAEMON_PID" ]; then
  echo "Daemon server is not running!"
  exit 1
fi
# There is an option: -a
if [ "$1" != "-a" ]; then
# Check if PID file is used 
  if [ -f "daemon.pid" ]; then
    DAEMON_PID="`cat daemon.pid`"
    if [ -z "$DAEMON_PID" ]; then
      echo "Incorrect daemon.pid file"
      exit 2
    fi
    rm daemon.pid
  else
    if [ -n "$DAEMON_PID" ]; then
      echo "daemon.pid is not found, but server processes are running"
      echo "To stop all of them, use -a option"
      exit 1
    fi
  fi
fi

echo "Stopping daemon server pid: $DAEMON_PID..."
kill $DAEMON_PID
sleep 1
exit 0
