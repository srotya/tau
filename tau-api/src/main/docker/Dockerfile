# Using centos 7 image
FROM java:8-jre
MAINTAINER Ambud Sharma

RUN apt-get -y update
RUN apt-get -y --force-yes install gettext mysql-client telnet wget netcat

RUN mkdir -p /usr/local/tau/
RUN mkdir -p /opt/tau/

RUN curl -o /usr/local/tau/api.jar "https://oss.sonatype.org/service/local/artifact/maven/content?r=releases&g=com.srotya.tau&a=tau-api&v=LATEST"

ADD ./config.yaml /opt/tau/
ADD ./template.properties /opt/tau/
ADD ./run.sh /opt/tau/

RUN chmod +x /opt/tau/*.sh

CMD /opt/tau/run.sh