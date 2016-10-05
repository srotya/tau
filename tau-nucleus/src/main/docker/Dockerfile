# Using centos 7 image
FROM java:8-jre
MAINTAINER Ambud Sharma

RUN mkdir -p /opt/tau/

ADD ./api.jar /opt/tau/nucleus.jar
ADD ./nucleus.yml /opt/tau/
ADD ./template.properties /opt/tau/
ADD ./start.sh /opt/tau/

RUN apt-get -y update
RUN apt-get -y --force-yes install gettext mysql-client telnet wget netcat
RUN chmod +x /opt/tau/*.sh

CMD /opt/tau/start.sh