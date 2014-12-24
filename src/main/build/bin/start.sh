#!/bin/bash

. "etc/env.conf"
CONLOG="${DAEMON_COUT:-"log/console.log"}"

# Check if server starts already
DAEMON_PID=`$PS_CMD | awk '/java .*-jar '$DAEMON_JAR'( '$DAEMON_ARGS')?$/{print$2}'`
if [ -n "$DAEMON_PID" ]; then
  echo "Daemon server is still running with pid: $DAEMON_PID!"
  exit 1
fi

echo "Starting daemon server..."
if [ "$1" == "-v" ]; then
  # Console output redirected to console.log for debugging
  nohup $JAVA_BIN $JAVA_OPTS -jar $DAEMON_JAR $DAEMON_ARGS > $CONLOG 2>&1 &
  echo "Redirecting console output to $CONLOG currently."
else
  rm -f $CONLOG
  nohup $JAVA_BIN $JAVA_OPTS -jar $DAEMON_JAR $DAEMON_ARGS > /dev/null 2>&1 &
fi
sleep 1

# Get new server process ID
DAEMON_PID=`$PS_CMD | awk '/java .*-jar '$DAEMON_JAR'( '$DAEMON_ARGS')?$/{print$2}'`
if [ -z "$DAEMON_PID" ]; then
  echo "Daemon server may not start correctly! To debug, use -v argument."
  exit 2
else
  echo $DAEMON_PID > daemon.pid
fi

exit 0
