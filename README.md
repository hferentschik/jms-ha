JMS HA with Wildfly 10

# Prerequesite

* Maven
* Docker
* Weave

# Setup

    > cd wildfly-10-server-1/
    > docker build --rm -t wildfly/server1 .
    > cd wildfly-10-server-2/
    > docker build --rm -t wildfly/server2 .

    > weave launch && weave launch-dns
    > weave launch-proxy --tls --tlscacert /Users/hardy/.boot2docker/certs/boot2docker-vm/ca.pem --tlscert /Users/hardy/.boot2docker/certs/boot2docker-vm/cert.pem --tlskey  /Users/hardy/.boot2docker/certs/boot2docker-vm/key.pem
    > eval $(weave proxy-env)


# Running the examples

    > docker run --rm --name server1 -t -i wildfly/server1
    > docker run --rm --name server2 -t -i wildfly/server2

    > docker run -i -t -v /Users/hardy/work/hibernate/git/jms-ha:/work hardy/jmsclient  /bin/bash


# What is important for JMS HA

* Redundant JMS Broker
 * Artemis in Wildfly 10
* Message grouping
* Transactional messages

# TODO

* Re-factor JMS Client code
* Introduce `MessageProvider`
* Implement message receiver in independent `Thread`
