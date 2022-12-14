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
	 * Whenever a message is received from a client, this method is called with the received message and the connection of the sender.
	 * 
	 * @param message The received message. Note that the message doesn't have any generic types.
	 * @param sender  The connection of the sender.
	 */
	public abstract void onMessageReceived(Message<?, ?> message, Connection sender);
	/**
	 * This method is called whenever a client connects to this server. The connect, the connection has to pass the {@link #acceptConnection(Connection) acceptConnection} check.
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
	 * Return {@code true} means the server will accept the connection, {@code false} means it will reject it.
	 * 
	 * @param connection The inbound connection.
	 * 
	 * @return {@code true} if the connection should be accepted, {@code false} if it should be rejected.
	 */
	protected boolean acceptConnection(Connection connection) {
		return true;
	}
	
	/**
	 * Starts the server.<br>
	 * Calling this method on a running server just returns {@code true}.
	 * 
	 * @return {@code true} if the server could be started, {@code false} otherwise.
	 * 
	 * @throws IOException The exception when an error occurs while starting the server.
	 */
	public synchronized boolean start() throws IOException {
		if(isRunning()) {
			return true;
		}
		
		synchronized (this) {
			System.out.print("starting...\r");
			
			this.socket = new ServerSocket(port);
			connectionAccepter = new Thread(this::waitForNewConnections, "waitForNewConnections");
			connectionAccepter.start();
			messageListener = new Thread(this::waitForMessages, "waitForMessages");
			messageListener.start();
			
			System.out.printf("server at port %d started%n", getPort());
		}
		return true;
	}
	
	/**
	 * Shutdowns this server.<br>
	 * Calling this method on a shut down server just returns {@code true}.
	 * 
	 * @return {@code true} if the server has been successfully shut down, {@code false} otherwise.
	 * 
	 * @throws IOException The exception, when an error occurs during shutdown.
	 */
	public synchronized boolean shutdown() throws IOException {
		if(!isRunning()) {
			return true;
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
		return true;
	}
	
	/**
	 * Closes the connection to all connected clients.
	 */
	private void closeAllConnections() {
		connectedClients.forEach(Connection::disconnect);
	}
	
	/**
	 * Adds a message to the received message message queue.
	 * 
	 * @param message The message to add to the queue.
	 * 
	 * @return {@code true} if the message could be added to the queue, {@code false} otherwise.
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
					new Thread(() -> onMessageReceived(message.message, message.sender), "onMessageReceived").start();;
				}
			}
		} catch(InterruptedException exc) {
			// thrown when the server is shutting down
		}
	}
	
	/**
	 * Sends a message to all connected clients.<br>
	 * Calling this method on a shut down server just returns {@code false}.
	 * 
	 * @param message The message to send, excluding the sender. (using {@code null} as the sender, sends it to everyone).
	 * 
	 * @return {@code true} if the message could be broadcasted, {@code false} otherwise.
	 */
	public synchronized boolean broadcastMessage(MessageToSend message) {
		if(!isRunning()) {
			return false;
		}
		for(Connection conn : connectedClients) {
			if(conn.equals(message.sender)) {
				continue;
			}
			
			conn.sendMessage(message.message);
		}
		return true;
	}
	
	/**
	 * Waits for a new connection and, if the connection is accepted via {@link #acceptConnection(Connection) acceptConnection}, is added to it's connected clients.
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
						
						// put this thread back to waiting
						// use a new one for handling, in case onClientConnected is a long task
						new Thread(() -> onClientConnected(conn), "onClientConnected").start();
					} else {
						conn.disconnect();
					}
				}
			} catch(IOException exc) {
				if(!(exc instanceof SocketException && exc.getMessage().equals("Socket closed"))) {
					// no idea what can cause this
					System.out.println("[Server][Warning] error during connection accepting");
					exc.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Removes the given connection from this server. Calls the {@link Connection#disconnect() disconnect} method of the connection.
	 * 
	 * @param conn The connection to remove.
	 * 
	 * @return {@code true} if the connection has been closed and removed, {@code false} otherwise.
	 */
	protected boolean removeConnection(Connection conn) {
		if(connectedClients.contains(conn)) {
			conn.disconnect();
			connectedClients.remove(conn);
			new Thread(() -> onClientDisconnected(conn), "onClientDisconnected").start();
			return true;
		} else {
			System.out.printf("[Server][Warning] connection (%s) is not a connected client%n", conn.toStringWithoutServer());
			return false;
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
	 * Changes the port of this server. It can only be changed, if the server is shut down.<br>
	 * Calling this method on a shut down server just returns {@code false}.
	 * 
	 * @param newPort The new port of this server.
	 * 
	 * @return {@code true} if the port has been changed, {@code false} if no change has been made.
	 */
	public boolean setPort(int newPort) {
		// can only be changed when entire server is controlled
		synchronized (this) {
			if(isRunning()) {
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
			return socket.getLocalPort() == server.socket.getLocalPort() && isRunning() == server.isRunning();
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Server{socketStatus=" + isRunning() + ", port=" + socket.getLocalPort() + ", connectedClients=" + connectedClients.size() + ", messagesToSend=" + messagesToSend.size() + "}";
	}
}
