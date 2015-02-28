#!/bin/bash
service tomcat6 stop
cp tilt.war /var/lib/tomcat6/webapps/
rm -rf /var/lib/tomcat6/webapps/tilt
rm -rf /var/lib/tomcat6/work/Catalina/localhost/
service tomcat6 start
