#!/bin/bash

. "etc/env.conf"

if [ -f "daemon.pid" ]; then
  DAEMON_PIDF="`cat daemon.pid`"
  if [ -z "$DAEMON_PIDF" ]; then
    echo "Incorrect fq.pid file"
    exit 2
  fi
  DAEMON_PID="`$PS_CMD | awk '{print$2}' | grep $DAEMON_PIDF`"
  DAEMON_NUM="`$PS_CMD | awk '{print$2}' | grep -c $DAEMON_PIDF`"
else
  DAEMON_PID="`$PS_CMD | awk '/java .*-jar '$DAEMON_JAR'( '$DAEMON_ARGS')?$/{print$2}'`"
  DAEMON_NUM="`$PS_CMD | egrep 'java .*-jar '$DAEMON_JAR'( '$DAEMON_ARGS')?$' | grep -v grep | wc -l`"
fi
if [ -z "$DAEMON_PID" ]; then
  echo "Daemon server is not running now"
  exit 1
fi

echo "$DAEMON_NUM daemon server is running, pid=$DAEMON_PID"
exit 0
