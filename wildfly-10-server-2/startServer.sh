#!/usr/bin/bash

ip=`ip addr show ethwe | grep "inet " | sed "s/.*\(10.128.0..\)\/10.*/\1/"`
echo $ip
/opt/jboss/wildfly/bin/standalone.sh --server-config standalone-full-ha.xml -b $ip

