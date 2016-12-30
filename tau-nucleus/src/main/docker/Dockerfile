# Using centos 7 image
FROM java:8-jre
MAINTAINER Ambud Sharma

RUN apt-get -y update
RUN apt-get -y --force-yes install gettext mysql-client telnet wget netcat

ENV DATA_DIR /opt/tau/data

RUN mkdir -p /usr/local/tau/
RUN mkdir -p /opt/tau/data

RUN curl -o /usr/local/tau/nucleus.jar "https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=com.srotya.tau&a=tau-nucleus&v=LATEST"
ADD ./nucleus.yml /opt/tau/
ADD ./template.conf /opt/tau/
ADD ./start.sh /opt/tau/

RUN chmod +x /opt/tau/*.sh

CMD /opt/tau/start.sh