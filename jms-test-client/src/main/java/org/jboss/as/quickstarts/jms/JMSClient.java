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
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

@SuppressWarnings("unused")
public class JMSClient {
	private static final Logger log = Logger.getLogger( JMSClient.class.getName() );

	private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
	private static final String DEFAULT_DESTINATION = "jms/queue/hsearch";
	private static final String DEFAULT_USERNAME = "guest";
	private static final String DEFAULT_PASSWORD = "guest";
	private static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
	// TODO - probably need to specify a list of servers. One might be down.
	private static final String DEFAULT_PROVIDER_URL = "http-remoting://10.128.0.2:8080";
	private static final String DEFAULT_MESSAGE = "Message id {count}";

	private static final AtomicInteger counter = new AtomicInteger();

	private static String userName = DEFAULT_USERNAME;
	private static String password = DEFAULT_PASSWORD;
	private static String connectionFactoryName = DEFAULT_CONNECTION_FACTORY;
	private static String message = DEFAULT_MESSAGE;
	private static String destination = DEFAULT_DESTINATION;
	private static String providerUrl = DEFAULT_PROVIDER_URL;

	private static InitialContext initialContext;
	private static ConnectionFactory connectionFactory;
	private static Queue queue;

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().addShutdownHook( new JMSShutdownThread() );

		JMSClient jmsClient = new JMSClient();
		jmsClient.init();
		ShellFactory.createConsoleShell( "jms", "", jmsClient ).commandLoop();
	}

	@Command(description = "Sets the name of the connection factory (default '" + DEFAULT_CONNECTION_FACTORY + "')")
	public void connectionFactoryName(@Param(name = "name", description = "The JMS factory name") String factoryName) {
		JMSClient.connectionFactoryName = factoryName;
		updateConnectionFactory();
	}

	@Command(description = "Sets the JMS provider URL (default '" + DEFAULT_PROVIDER_URL + "')")
	public void providerURL(@Param(name = "provider") String provider) {
		JMSClient.providerUrl = provider;
	}

	@Command//(description = "Sets the user name (default '" + DEFAULT_USERNAME + "')")
	public void userName(@Param(name = "name", description = "The JMS user name") String userName) {
		JMSClient.userName = userName;
	}

	@Command//(description = "Sets the password (default '" + DEFAULT_PASSWORD + "')")
	public void password(@Param(name = "name", description = "The JMS password") String password) {
		JMSClient.password = password;
	}

	@Command(description = "Sets the JMS destination (default '" + DEFAULT_DESTINATION + "')")
	public void destination(@Param(name = "destination") String destination) {
		JMSClient.destination = destination;
		updateQueue();
	}

	@Command(description = "Sets the JMS message content (default '" + DEFAULT_MESSAGE + "')")
	public void message(@Param(name = "message") String message) {
		JMSClient.message = message;
	}

	@Command(description = "Print InitialContext info", abbrev = "p")
	public void printInitialContext() throws Exception {
		log.info( initialContext.toString() );
		log.info( initialContext.getEnvironment().toString() );
	}

	@Command(description = "Send the specified amount of messages", abbrev = "s")
	public void sendMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		try (Connection connection = createConnection();
			 Session session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
			 MessageProducer messageProducer = session.createProducer( queue )) {

			connection.start();

			// Send the specified number of messages
			TextMessage textMessage = session.createTextMessage();
			for ( int i = 0; i < count; i++ ) {
				String messageContent = message.replaceAll( "\\{count\\}", String.valueOf( counter.addAndGet( 1 ) ) );
				log.info( "Sending message with content: '" + messageContent + "'" );
				textMessage.setText( messageContent );
				messageProducer.send( textMessage );
			}
		}
	}

	@Command(description = "Recieve the specified amount of messages", abbrev = "r")
	public void receiveMessage(@Param(name = "count", description = "Message count") int count) throws Exception {
		try (Connection connection = createConnection();
			 Session session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
			 MessageConsumer messageConsumer = session.createConsumer( queue )) {

			connection.start();
			for ( int i = 0; i < count; i++ ) {
				TextMessage textMessage = (TextMessage) messageConsumer.receive();
				log.info( "Received message with content " + textMessage.getText() );
			}
		}
	}

	//@Command(description = "(Re-)Init InitialContext", abbrev = "i")
	private void init() throws Exception {
		try {
			createInitialContext();
			updateConnectionFactory();
			updateQueue();
		}
		catch ( Exception e ) {
			log.severe( e.toString() );
		}
	}

	private void createInitialContext() {
		// Set up the namingContext for the JNDI lookup
		final Properties env = new Properties();
		env.put( Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY );
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

	private void updateQueue() {
		try {
			log.info( "Attempting to lookup queue \"" + destination + "\"" );
			JMSClient.queue = (Queue) initialContext.lookup( destination );
			log.info( "Found queue \"" + queue.toString() + "\" in JNDI" );
		}
		catch ( NamingException e ) {
			log.severe( e.toString() );
		}
	}

	private void updateConnectionFactory() {
		try {
			log.info( "Attempting to lookup connection factory \"" + connectionFactoryName + "\"" );
			JMSClient.connectionFactory = (ConnectionFactory) initialContext.lookup( connectionFactoryName );
			log.info( "Found connection factory \"" + connectionFactory.toString() + "\" in JNDI" );
		}
		catch ( NamingException e ) {
			log.severe( e.toString() );
		}
	}

	private Connection createConnection() throws Exception {
		Connection connection;
		if ( userName != null && password != null ) {
			connection = connectionFactory.createConnection( userName, password );
		}
		else {
			connection = connectionFactory.createConnection();
		}
		return connection;
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
		}
	}
}

