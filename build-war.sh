#!/bin/bash
if [ ! -d tilt ]; then
  mkdir tilt
  if [ $? -ne 0 ] ; then
    echo "couldn't create tilt directory"
    exit
  fi
fi
if [ ! -d tilt/WEB-INF ]; then
  mkdir tilt/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create tilt/WEB-INF directory"
    exit
  fi
fi
if [ ! -d tilt/static ]; then
  mkdir tilt/static
  if [ $? -ne 0 ] ; then
    echo "couldn't create tilt/static directory"
    exit
  fi
fi
if [ ! -d tilt/WEB-INF/lib ]; then
  mkdir tilt/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create tilt/WEB-INF/lib directory"
    exit
  fi
fi
rm -f tilt/WEB-INF/lib/*.jar
cp dist/TILT2.jar tilt/WEB-INF/lib/
cp lib/*.jar tilt/WEB-INF/lib/
cp *.js tilt/static/
cp polygon/* tilt/static/
cp web.xml tilt/WEB-INF/
jar cf tilt.war -C tilt WEB-INF -C tilt static
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
