FROM centos:centos6
MAINTAINER Mazen Kotb

RUN yum -y install wget gcc openssl openssl-devel
RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.rpm
RUN rpm -Uvh jdk-8u25-linux-x64.rpm

RUN mkdir /minecloud
ADD initialize.sh /minecloud/initialize.sh

EXPOSE 25565

WORKDIR /minecloud
ENTRYPOINT ["sh", "initialize.sh"]