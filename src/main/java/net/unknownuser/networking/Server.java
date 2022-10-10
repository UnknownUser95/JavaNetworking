package net.unknownuser.networking;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class Server {
	private int port;
	protected ServerSocket socket;
	protected final ArrayList<Connection> connectedClients = new ArrayList<>();
	protected final LinkedBlockingQueue<MessageToSend> messagesToSend = new LinkedBlockingQueue<>();
	protected boolean running = false;
	protected Thread messageListener = null;
	protected Thread connectionAccepter = null;
	
	protected Server(int port) {
		super();
		this.port = port;
	}
	
	/**
	 * Whenever a message is received from a client, this method is called with the received message
	 * and the connection of the sender.
	 * 
	 * @param message The received message. Note that the message doesn't have any generic types.
	 * @param sender  The connection of the sender.
	 */
	public abstract void onMessageReceived(Message<?, ?> message, Connection sender);
	/**
	 * This method is called whenever a client connects to this server. The connect, the connection
	 * has to pass the {@link #acceptConnection(Connection) acceptConnection} check.
	 * 
	 * @param client The newly connected client.
	 */
	public abstract void onClientConnected(Connection client);
	/**
	 * Whenever a client disconnects this method is called.
	 * 
	 * @param client The disconnected client connection.
	 */
	public abstract void onClientDisconnected(Connection client);
	
	/**
	 * Whether the connection should be accepted or not.<br>
	 * Return {@code true} means the server will accept the connection, {@code false} means it will
	 * reject it.
	 * 
	 * @param connection The inbound connection.
	 * @return {@code true} if the connection should be accepted, {@code false} if it should be
	 *         rejected.
	 */
	protected boolean acceptConnection(Connection connection) {
		return true;
	}
	
	public synchronized void start() throws IOException {
		this.socket = new ServerSocket(port);
		connectionAccepter = new Thread(this::waitForNewConnections);
		connectionAccepter.start();
		messageListener = new Thread(this::waitForMessages);
		messageListener.start();
		running = true;
		System.out.printf("server at port %d started%n", getPort());
	}
	
	public synchronized void shutdown() {
		if(!running) {
			return;
		}
		
		try {
			System.out.print("shutting down...\r");
			// stop threads
			messageListener.interrupt();
			connectionAccepter.interrupt();
			
			// clear message queue
			messagesToSend.clear();
			// close all connections and socket
			closeAllConnections();
			while(!connectedClients.isEmpty()) {
				// wait until all connections are closed
			}
			socket.close();
			running = false;
			System.out.printf("server at port %d shut down%n", getPort());
		} catch(IOException exc) {
			System.err.println("couldn't shutdown server");
			System.err.println(exc.getMessage());
		}
	}
	
	private void closeAllConnections() {
		connectedClients.forEach(Connection::disconnect);
	}
	
	public void addMessageToQueue(MessageToSend message) {
		if(!messagesToSend.offer(message)) {
			System.err.println("could not add message");
		}
	}
	
	private void waitForMessages() {
		try {
			while(!socket.isClosed()) {
				MessageToSend message = messagesToSend.take();
				if(message != null) {
					onMessageReceived(message.message, message.sender);
				}
			}
		} catch(InterruptedException exc) {
			// thrown when the server is shutting down, but just in case
			shutdown();
		}
	}
	
	public synchronized void broadcastMessage(MessageToSend message) {
		for(Connection conn : connectedClients) {
			if(conn.equals(message.sender)) {
				continue;
			}
			
			conn.sendMessage(message.message);
		}
	}
	
	private void waitForNewConnections() {
		while(!socket.isClosed()) {
			try {
				Socket connection = socket.accept();
				if(connection != null) {
					Connection conn = new Connection(connection, this);
					if(acceptConnection(conn)) {
						connectedClients.add(conn);
						Thread clientThread = new Thread(conn);
						clientThread.setDaemon(true);
						clientThread.start();
						
						onClientConnected(conn);
					} else {
						conn.disconnect();
					}
				}
			} catch(IOException exc) {
				if(!(exc instanceof SocketException && exc.getMessage().equals("Socket closed"))) {
					System.out.println("[Server][Warning] error during connection accepting");
					System.out.println(exc.getMessage());
					// no idea what can cause this, shut down just in case
					shutdown();
				}
			}
		}
	}
	
	protected void removeConnection(Connection conn) {
		conn.disconnect();
		if(connectedClients.contains(conn)) {
			connectedClients.remove(conn);
			onClientDisconnected(conn);
		} else {
			System.err.printf("[Server][Warning]: couldn't remove connection (%s)%n", conn);
		}
	}
	
	public List<Connection> getConnectedClients() {
		return connectedClients;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean setPort(int newPort) {
		if(isRunning()) {
			return false;
		} else {
			port = newPort;
			return true;
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		
		if(obj == this) {
			return true;
		}
		
		if(obj instanceof Server server) {
			return socket.equals(server.socket);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Server{socketStatus=" + !socket.isClosed() + ", port=" + socket.getLocalPort() + ", connectedClients=" + connectedClients.size() + ", messagesToSend=" + messagesToSend.size() + "}";
	}
}
