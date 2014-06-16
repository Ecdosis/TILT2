#!/bin/bash
getjarpaths()
{
  JARPATH=""
  for f in $1/*.jar
  do
    JARPATH="$JARPATH:$f"
  done
  echo $JARPATH
  return
}
if [ "`uname`" = "Darwin" ]; then
  WEB_ROOT="/Library/WebServer/Documents"
else
  WEB_ROOT="/var/www"
fi
JARPATHS=`getjarpaths lib`
if [ `uname` = "Darwin" ]; then
  pgrep(){ ps -ax -o pid,command | grep "$@" | grep -v 'grep' | awk '{print $1;}'; }
  HPID=`pgrep TILT2.jar`
  if [ -n "$HPID" ]; then
    kill $HPID
  fi
else
  pkill -f TILT2.jar
fi
nohup java -cp .$JARPATHS:TILT2.jar tilt.JettyServer &

