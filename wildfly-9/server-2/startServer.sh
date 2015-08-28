#!/usr/bin/bash

ip=`ip addr show eth0 | grep "inet " | sed "s/.*\(172.17.0...\)\/16.*/\1/"`
echo $ip
/opt/jboss/wildfly/bin/standalone.sh --server-config standalone-full-ha.xml -b $ip

