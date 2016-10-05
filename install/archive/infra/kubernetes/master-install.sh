#!/bin/bash

sudo yum -y update
sudo yum -y install nano wget flannel kubernetes ntp etcd net-tools

sudo cp /opt/configs/etcd.conf /etc/etcd/etcd.conf
sudo cp /opt/configs/apiserver /etc/kubernetes/apiserver

for SERVICES in etcd kube-apiserver kube-controller-manager kube-scheduler; do
    sudo systemctl restart $SERVICES
    sudo systemctl enable $SERVICES
    sudo systemctl status $SERVICES 
done

etcdctl mk /atomic.io/network/config '{"Network":"172.17.0.0/16"}'

kubectl get nodes
