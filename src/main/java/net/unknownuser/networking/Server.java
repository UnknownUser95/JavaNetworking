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
	
	/**
	 * Starts the server.
	 * 
	 * @throws IOException The exception when an error occurs while starting the server.
	 */
	public synchronized void start() throws IOException {
		if(isRunning()) {
			System.err.println("could not start, server is already running");
			return;
		}
		
		synchronized (this) {
			this.socket = new ServerSocket(port);
			connectionAccepter = new Thread(this::waitForNewConnections, "waitForNewConnections");
			connectionAccepter.start();
			messageListener = new Thread(this::waitForMessages, "waitForMessages");
			messageListener.start();
			System.out.printf("server at port %d started%n", getPort());
		}
	}
	
	/**
	 * Shutdowns this server. Calling it after the server has been shut down doesn't do anything.
	 * 
	 * @throws IOException The exception, when an error occurs during shutdown.
	 */
	public synchronized void shutdown() throws IOException {
		if(!isRunning()) {
			System.err.println("could not shutdown, server is already shut down");
			return;
		}
		
		synchronized (this) {
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
				System.out.printf("server at port %d shut down%n", getPort());
			} catch(IOException exc) {
				System.err.println("couldn't shutdown server");
				throw exc;
			}
		}
	}
	
	/**
	 * Closes the connection to all connected clients.
	 */
	private void closeAllConnections() {
		connectedClients.forEach(Connection::disconnect);
	}
	
	/**
	 * Adds a message to the message queue.
	 * 
	 * @param message The message to add to the queue.
	 */
	public boolean addMessageToQueue(MessageToSend message) {
		return messagesToSend.offer(message);
	}
	
	/**
	 * Waits for new messages to send and processes them.
	 */
	private void waitForMessages() {
		try {
			while(isRunning()) {
				MessageToSend message = messagesToSend.take();
				if(message != null) {
					onMessageReceived(message.message, message.sender);
				}
			}
		} catch(InterruptedException exc) {
			// thrown when the server is shutting down
		}
	}
	
	/**
	 * Sends a message to all connected clients.
	 * 
	 * @param message The message to send, excluding the sender. (using {@code null} as the sender,
	 *                sends it to everyone).
	 */
	public synchronized void broadcastMessage(MessageToSend message) {
		if(!isRunning()) {
			System.err.println("could not broadcast message, server is not running");
			return;
		}
		
		for(Connection conn : connectedClients) {
			if(conn.equals(message.sender)) {
				continue;
			}
			
			conn.sendMessage(message.message);
		}
	}
	
	/**
	 * Waits for a new connection and, if the connection is accepted via
	 * {@link #acceptConnection(Connection) acceptConnection}, is added to it's connected clients.
	 */
	private void waitForNewConnections() {
		while(isRunning()) {
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
					exc.printStackTrace();
					// no idea what can cause this
				}
			}
		}
	}
	
	/**
	 * Removes the given connection from this server. Calls the {@link Connection#disconnect()
	 * disconnect} method of the connection.
	 * 
	 * @param conn The connection to remove.
	 */
	protected void removeConnection(Connection conn) {
		if(connectedClients.contains(conn)) {
			conn.disconnect();
			connectedClients.remove(conn);
			onClientDisconnected(conn);
		} else {
			System.out.printf("[Server][Warning] connection (%s) is not a connected client%n", conn);
		}
	}
	
	/**
	 * Gets the list of currently connected clients.
	 * 
	 * @return The connected clients.
	 */
	public List<Connection> getConnectedClients() {
		return connectedClients;
	}
	
	/**
	 * Gets the port, which the server is using.
	 * 
	 * @return This server's port.
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Changes the port of this server. It can only be changed, if the server is shut down.
	 * 
	 * @param newPort The new port of this server.
	 * @return {@code true} if the port has been changed, {@code false} if no change has been made.
	 */
	public boolean setPort(int newPort) {
		// can only be changed when entire server is controlled
		synchronized (this) {
			if(isRunning()) {
				System.err.println("could not change port, server is running");
				return false;
			} else {
				port = newPort;
				return true;
			}			
		}
	}
	
	/**
	 * Returns whether this server is currently running.
	 * 
	 * @return The status of this server.
	 */
	public boolean isRunning() {
		return socket != null && !socket.isClosed();
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
		return "Server{socketStatus=" + isRunning() + ", port=" + socket.getLocalPort() + ", connectedClients=" + connectedClients.size() + ", messagesToSend=" + messagesToSend.size() + "}";
	}
}
