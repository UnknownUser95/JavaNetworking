package net.unknownuser.networking;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class Server {
	private final int port;
	protected ServerSocket socket;
	protected final ArrayList<Connection> connectedClients = new ArrayList<>();
	protected final LinkedBlockingQueue<MessageToSend> messagesToSend = new LinkedBlockingQueue<>();
	
	protected Server(int port) {
		super();
		this.port = port;
	}
	
	public abstract void onMessageReceived(MessageToSend message);
	public abstract void onClientConnected(Connection client);
	public abstract void onClientDisconnected(Connection client);
	
	public void start() throws IOException {
		this.socket = new ServerSocket(port);
		Thread connectionAccepter = new Thread(this::waitForNewConnections);
		connectionAccepter.start();
		Thread messageListener = new Thread(this::waitForMessages);
		messageListener.start();
		System.out.println("server started at port " + getPort());
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
				
				onMessageReceived(message);
			}
		} catch(InterruptedException exc) {
			System.err.println("server interrupted");
			exc.printStackTrace();
		}
	}
	
	public void broadcastMessage(MessageToSend message) {
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
					connectedClients.add(conn);
					Thread clientThread = new Thread(conn);
					clientThread.setDaemon(true);
					clientThread.start();
					
					onClientConnected(conn);
				}
			} catch(IOException exc) {
				System.out.println("[Server][Warning] error during connection accepting");
				System.out.println(exc.getMessage());
			}
		}
	}
	
	protected void removeConnection(Connection conn) {
		if(connectedClients.contains(conn)) {
			conn.disconnect();
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
