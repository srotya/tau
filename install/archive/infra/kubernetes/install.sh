#!/bin/bash

sudo yum -y update
sudo yum -y install nano wget flannel kubernetes ntp net-tools gettext

sudo echo "FLANNEL_ETCD=\"http://192.168.99.101:2379\"" >> /etc/sysconfig/flanneld

sudo echo "KUBE_MASTER=\"--master=http://192.168.99.101:8080\"">> /etc/kubernetes/config

export IP=`/sbin/ifconfig enp0s8 | grep 'inet' | cut -d: -f2 | awk '{ print $2}'`

echo "IP address is $IP"

envsubst < /opt/configs/kubelet > /tmp/kubelet

sudo cp /tmp/kubelet /etc/kubernetes/kubelet

for SERVICES in kube-proxy kubelet docker flanneld; do
    sudo systemctl restart $SERVICES
    sudo systemctl enable $SERVICES
    sudo systemctl status $SERVICES 
done
