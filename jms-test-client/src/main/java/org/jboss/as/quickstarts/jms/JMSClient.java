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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;
import org.hornetq.jms.client.HornetQDestination;
import org.hornetq.jms.server.embedded.EmbeddedJMS;

@SuppressWarnings("unused")
public class JMSClient {
	private static final Logger log = Logger.getLogger( JMSClient.class.getName() );

	// Set up all the default values
	private static final String DEFAULT_MESSAGE = "Message id {count}";
	private static final String DEFAULT_CONNECTION_FACTORY = "ConnectionFactory";
	private static final String DEFAULT_DESTINATION = "jms/queue/test";
	private static final String DEFAULT_USERNAME = "guest";
	private static final String DEFAULT_PASSWORD = "guest";
	private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
	private static final String PROVIDER_URL = "http-remoting://127.0.0.1:8080";
	private static final AtomicInteger counter = new AtomicInteger();

	private static String userName = DEFAULT_USERNAME;
	private static String password = DEFAULT_PASSWORD;
	private static String connectionFactoryName = DEFAULT_CONNECTION_FACTORY;
	private static String message = DEFAULT_MESSAGE;
	private static String destination = DEFAULT_DESTINATION;
	private static String providerUrl = PROVIDER_URL;

	private static InitialContext initialContext;
	private static ConnectionFactory connectionFactory;
	private static Destination jmsDestination;
	private static EmbeddedJMS jms;

	@Command(description = "Sets the name of the connection factory (default '" + DEFAULT_CONNECTION_FACTORY + "')")
	public void connectionFactoryName(@Param(name = "name", description = "The JMS factory name") String factoryName) {
		JMSClient.connectionFactoryName = factoryName;
	}

	@Command(description = "Sets the user name (default '" + DEFAULT_USERNAME + "')")
	public void userName(@Param(name = "name", description = "The JMS user name") String userName) {
		JMSClient.userName = userName;
	}

	@Command(description = "Sets the password (default '" + DEFAULT_PASSWORD + "')")
	public void password(@Param(name = "name", description = "The JMS password") String password) {
		JMSClient.password = password;
	}

	@Command(description = "Sets the JMS message content (default '" + DEFAULT_MESSAGE + "')")
	public void message(@Param(name = "message") String message) {
		JMSClient.message = message;
	}

	@Command(description = "Sets the JMS destination (default '" + DEFAULT_DESTINATION + "')")
	public void destination(@Param(name = "destination") String destination) {
		JMSClient.destination = destination;
	}

	@Command(description = "Sets the JMS provider URL (default '" + PROVIDER_URL + "')")
	public void providerURL(@Param(name = "provider") String provider) {
		JMSClient.providerUrl = provider;
	}

	@Command(description = "(Re-)Init InitialContext", abbrev = "i")
	public void init() throws Exception {
		try {
			createInitialContext();

			log.info( "Attempting to acquire connection factory \"" + connectionFactoryName + "\"" );
			//JMSClient.connectionFactory = (ConnectionFactory) initialContext.lookup( connectionFactoryName );
			JMSClient.connectionFactory = (ConnectionFactory) jms.lookup( "ConnectionFactory" );
			log.info( "Found connection factory \"" + connectionFactoryName + "\" in JNDI" );
		}
		catch ( Exception e ) {
			log.severe( e.toString() );
		}
	}

	@Command(description = "Print InitialContext info", abbrev = "p")
	public void printInitialContext() throws Exception {
		log.info( initialContext.toString() );
		log.info( initialContext.getEnvironment().toString() );
	}

	@Command(description = "Send the specified amount of messages", abbrev = "s")
	public void sendMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		Destination destination = getDestination();

		//Destination destination = (Destination)jms.lookup(DEFAULT_DESTINATION);
//		final String groupAddress = "231.7.7.7";
//		final int groupPort = 9876;
//		ServerLocator locator = HornetQClient.createServerLocatorWithHA(
//				new DiscoveryGroupConfiguration(
//						groupAddress, groupPort,
//						10000,
//						new UDPBroadcastGroupConfiguration( groupAddress, groupPort, "127.0.0.1", -1 )
//				)
//		);
//		ClientSessionFactory factory = locator.createSessionFactory();
//		ClientSession session1 = factory.createSession();


		try (JMSContext context = connectionFactory.createContext( userName, password )) {
			// Send the specified number of messages
			for ( int i = 0; i < count; i++ ) {
				String messageContent = message.replaceAll( "\\{count\\}", String.valueOf( counter.addAndGet( 1 ) ) );
				log.info( "Sending message with content: '" + messageContent + "'" );
				context.createProducer().send( destination, messageContent );
			}
		}
	}

	@Command(description = "Recieve the specified amount of messages", abbrev = "r")
	public void receiveMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		Destination destination = getDestination();

		try (JMSContext context = connectionFactory.createContext( userName, password )) {
			// Create the JMS consumer
			JMSConsumer consumer = context.createConsumer( destination );
			// Then receive the same number of messages that were sent
			for ( int i = 0; i < count; i++ ) {
				String text = consumer.receiveBody( String.class, 5000 );
				log.info( "Received message with content " + text );
			}
		}
	}

	public static void main(String[] args) throws Exception {
		jms = new EmbeddedJMS();
		jms.start();

		Runtime.getRuntime().addShutdownHook( new JMSShutdownThread() );

		JMSClient jmsClient = new JMSClient();
		jmsClient.init();
		ShellFactory.createConsoleShell( "jms", "", jmsClient ).commandLoop();
	}

	private void createInitialContext() {
		// Set up the namingContext for the JNDI lookup
		final Properties env = new Properties();
		env.put( Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY );
		env.put( Context.PROVIDER_URL, providerUrl );
		env.put( Context.SECURITY_PRINCIPAL, userName );
		env.put( Context.SECURITY_CREDENTIALS, password );
		try {
			initialContext = new InitialContext( env );
		}
		catch ( NamingException e ) {
			log.severe( e.toString() );
		}
	}

	private Destination getDestination() throws NamingException {
		//return HornetQDestination.fromAddress( destination );
		return (Destination) initialContext.lookup( destination );
	}

	public static class JMSShutdownThread extends Thread {
		public JMSShutdownThread() {
			super();
			this.setName( "JMSShutdownThread" );
		}

		public void run() {
			InitialContext initialContext = JMSClient.initialContext;
			if ( initialContext != null ) {
				try {
					System.out.println( "Closing initial context" );
					initialContext.close();
				}
				catch ( NamingException e ) {
					// ingnore
				}
			}

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

