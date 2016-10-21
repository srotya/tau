# Using centos 7 image
FROM java:8-jre
MAINTAINER asharma525xi@gmail.com

RUN apt-get -y update
RUN apt-get -y --force-yes install gettext mysql-client telnet wget netcat

RUN mkdir -p /usr/local/hendrix/
RUN mkdir -p /opt/hendrix/

ADD ./api.jar /usr/local/hendrix/api.jar
ADD ./config.yaml /opt/hendrix/
ADD ./template.properties /opt/hendrix/
ADD ./run.sh /opt/hendrix/

RUN chmod +x /opt/hendrix/*.sh

CMD /opt/hendrix/run.sh