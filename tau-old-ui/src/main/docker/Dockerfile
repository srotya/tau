# Using centos 7 image
FROM tomcat:8-jre8
MAINTAINER Ambud Sharma

RUN apt-get -y update
RUN apt-get -y --force-yes install gettext telnet

RUN mkdir -p /opt/tau
RUN rm -rf /usr/local/tomcat/webapps/*

ADD ./ROOT.war /usr/local/tomcat/webapps/ROOT.war
ADD ./template.properties /opt/tau/
ADD ./run.sh /opt/tau/

RUN chmod +x /opt/tau/run.sh
CMD /opt/tau/run.sh