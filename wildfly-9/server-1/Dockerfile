FROM jboss/base-jdk:8

# User root user to install software
USER root

# Install necessary packages
RUN yum -y install omping

# Switch back to jboss user
USER jboss

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION 9.0.0.Final
ENV JBOSS_HOME /opt/jboss/wildfly

# Add the WildFly distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd $HOME \
    && curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz


# Copy Wildfly config files
COPY standalone-full-ha-1.xml /opt/jboss/wildfly/standalone/configuration/standalone-full-ha.xml
COPY application-roles.properties /opt/jboss/wildfly/standalone/configuration/application-roles.properties
COPY application-users.properties /opt/jboss/wildfly/standalone/configuration/application-users.properties
COPY startServer.sh /opt/jboss/startServer.sh

USER root
RUN chown -R jboss:jboss /opt/jboss
RUN echo "root:Docker!" | chpasswd
USER jboss

RUN chmod u+x /opt/jboss/startServer.sh

# Expose the ports we're interested in
EXPOSE 8080

# Set the default command to run on boot
CMD ["/opt/jboss/startServer.sh"]
