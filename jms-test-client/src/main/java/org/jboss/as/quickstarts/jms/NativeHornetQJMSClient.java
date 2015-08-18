/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.jms;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;
import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.UDPBroadcastGroupConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.jms.server.embedded.EmbeddedJMS;

@SuppressWarnings("unused")
public class NativeHornetQJMSClient {
	private static final Logger log = Logger.getLogger( JMSClient.class.getName() );

	// Set up all the default values
	private static final String DEFAULT_MESSAGE = "Message id {count}";
	private static final String DEFAULT_CONNECTION_FACTORY = "ConnectionFactory";
	private static final String DEFAULT_DESTINATION = "jms/queue/test";
	private static final String DEFAULT_USERNAME = "guest";
	private static final String DEFAULT_PASSWORD = "guest";
	private static final AtomicInteger counter = new AtomicInteger();

	private static String userName = DEFAULT_USERNAME;
	private static String password = DEFAULT_PASSWORD;
	private static String connectionFactoryName = DEFAULT_CONNECTION_FACTORY;
	private static String message = DEFAULT_MESSAGE;
	private static String destination = DEFAULT_DESTINATION;

	private static ConnectionFactory connectionFactory;
	private static Destination jmsDestination;
	private static EmbeddedJMS jms;

	@Command(description = "Sets the name of the connection factory (default '" + DEFAULT_CONNECTION_FACTORY + "')")
	public void connectionFactoryName(@Param(name = "name", description = "The JMS factory name") String factoryName) {
		NativeHornetQJMSClient.connectionFactoryName = factoryName;
	}

	@Command(description = "Sets the user name (default '" + DEFAULT_USERNAME + "')")
	public void userName(@Param(name = "name", description = "The JMS user name") String userName) {
		NativeHornetQJMSClient.userName = userName;
	}

	@Command(description = "Sets the password (default '" + DEFAULT_PASSWORD + "')")
	public void password(@Param(name = "name", description = "The JMS password") String password) {
		NativeHornetQJMSClient.password = password;
	}

	@Command(description = "Sets the JMS message content (default '" + DEFAULT_MESSAGE + "')")
	public void message(@Param(name = "message") String message) {
		NativeHornetQJMSClient.message = message;
	}

	@Command(description = "Sets the JMS destination (default '" + DEFAULT_DESTINATION + "')")
	public void destination(@Param(name = "destination") String destination) {
		NativeHornetQJMSClient.destination = destination;
	}

	@Command(description = "(Re-)Init InitialContext", abbrev = "i")
	public void init() throws Exception {
		try {
			NativeHornetQJMSClient.connectionFactory = (ConnectionFactory) jms.lookup( "ConnectionFactory" );
		}
		catch ( Exception e ) {
			log.severe( e.toString() );
		}
	}

	@Command(description = "Send the specified amount of messages", abbrev = "s")
	public void sendMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		try (ClientSession session = createSession();
			 ClientProducer messageProducer = session.createProducer( DEFAULT_DESTINATION )) {

			// Send the specified number of messages
			ClientMessage textMessage = session.createMessage( true );
			for ( int i = 0; i < count; i++ ) {
				String messageContent = message.replaceAll( "\\{count\\}", String.valueOf( counter.addAndGet( 1 ) ) );
				log.info( "Sending message with content: '" + messageContent + "'" );
//				textMessage.setText( messageContent );
				messageProducer.send( textMessage );
			}
		}
	}

	@Command(description = "Recieve the specified amount of messages", abbrev = "r")
	public void receiveMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		try (ClientSession session = createSession();
			 ClientConsumer messageConsumer = session.createConsumer( DEFAULT_DESTINATION )) {

			for ( int i = 0; i < count; i++ ) {
				TextMessage textMessage = (TextMessage) messageConsumer.receive();
				log.info( "Received message with content " + textMessage.getText() );
			}
		}
	}

	public static void main(String[] args) throws Exception {
		jms = new EmbeddedJMS();
		jms.start();

		Runtime.getRuntime().addShutdownHook( new JMSShutdownThread() );

		NativeHornetQJMSClient jmsClient = new NativeHornetQJMSClient();
		jmsClient.init();
		ShellFactory.createConsoleShell( "jms", "", jmsClient ).commandLoop();
	}

	private Destination getDestination() throws NamingException {
		return (Destination) jms.lookup( destination );
	}

	private ClientSession createSession() throws Exception {
		final String groupAddress = "231.7.7.7";
		final int groupPort = 9876;
		ServerLocator locator = HornetQClient.createServerLocatorWithHA(
				new DiscoveryGroupConfiguration(
						groupAddress, groupPort,
						10000,
						new UDPBroadcastGroupConfiguration( groupAddress, groupPort, "127.0.0.1", -1 )
				)
		);
		ClientSessionFactory factory = locator.createSessionFactory();
		return factory.createSession();
	}

	public static class JMSShutdownThread extends Thread {
		public JMSShutdownThread() {
			super();
			this.setName( "JMSShutdownThread" );
		}

		public void run() {
			if ( jms != null ) {
				try {
					System.out.println( "Closing embedded JMS" );
					jms.stop();
				}
				catch ( Exception e ) {
					// ignore
				}
			}
		}
	}
}

