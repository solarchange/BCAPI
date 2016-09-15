# BCAPI

1. Install tomcat8: sudo apt-get install (or sudo yum install if RedHat/CentOS) tomcat8
1.1  Install web apps for tomcat: sudo yum install tomcat-webapps tomcat-admin-webapps
2. Go to/var/lib/tomcat8/webapps , replace everything there with the ROOT.war from sources provided
3. Go to /usr/share/tomcat8/lib and place following files there: application.properties (it is found under src/main/resources from the sources provided) and MySQL connector JAR (mysql-connector-java-5.1.35.jar , from sources)
4. sudo /etc/init.d/tomcat8 - restart

5. install monit: sudo apt-get install monit
6. Edit the monit config file to watch tomcat8 (/etc/monitrc or if not found there look up in the ofdocs - https://mmonit.com/monit/documentation/monit.html)
7. Add following lines to the file:

check process tomcat8 with pidfile /var/run/tomcat8.pid 
   start program = "/etc/init.d/tomcat8 start" with timeout 120 seconds 
   stop program = "/etc/init.d/tomcat8 stop"

8. restart monit and tomcat8
I will send you the release pack now
it has all the needed files and execs
