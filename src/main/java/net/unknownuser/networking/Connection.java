package net.unknownuser.networking;

import java.io.*;
import java.net.*;

public class Connection implements Runnable {
	protected final Socket socket;
	protected final ObjectInputStream socketReader;
	protected final ObjectOutputStream socketWriter;
	protected final Server server;
	
	public Connection(Socket socket, Server server) throws IOException {
		super();
		this.socket = socket;
		this.socketWriter = new ObjectOutputStream(new DataOutputStream(socket.getOutputStream()));
		this.socketReader = new ObjectInputStream(new DataInputStream(socket.getInputStream()));
		this.server = server;
	}
	
	/**
	 * Starts to listen for incoming messages.<br>
	 * Entry point for threads.
	 */
	@Override
	public void run() {
		while(!socket.isClosed()) {
			receiveMessage();
		}
	}
	
	/**
	 * Waits for a message from the server and processes it.
	 */
	private void receiveMessage() {
		try {
			Message<?, ?> message = (Message<?, ?>) socketReader.readObject();
			server.addMessageToQueue(new MessageToSend(message, this));
		} catch(IOException exc) {
			if(!(exc instanceof EOFException) && !(exc instanceof SocketException && exc.getMessage().equals("Socket closed"))) {
				System.out.println("error while reading message");
				exc.printStackTrace();
			}
			server.removeConnection(this);
		} catch(ClassNotFoundException exc) {
			System.err.println("received object could not be mapped to a class");
		}
	}
	
	/**
	 * Sends a message to the connected server.
	 * 
	 * @param message The message to send.
	 */
	public void sendMessage(Message<?, ?> message) {
		synchronized (socket) {
			try {
				socketWriter.writeObject(message);
				socketWriter.flush();
			} catch(IOException exc) {
				System.out.println("error while sending message");
				System.out.println(exc.getMessage());
				server.removeConnection(this);
			}
		}
	}
	
	/**
	 * Closes all streams. This disconnects the client.<br>
	 * Do not use this instance after this method has been called.
	 */
	public void disconnect() {
		if(!socket.isClosed()) {
			synchronized (socket) {
				try {
					if(socketWriter != null) {
						socketWriter.close();
					}
					if(socketReader != null) {
						socketReader.close();
					}
					if(socket != null) {
						socket.close();
					}
				} catch(IOException exc) {
					System.err.println("error while closing");
					exc.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Gets the InetAddress of this connection.
	 * 
	 * @return The InetAdress of this connection.
	 */
	public InetAddress getAddress() {
		return socket.getInetAddress();
	}
	
	/**
	 * Gets the IP of the connected client.
	 * 
	 * @return The IP as a string.
	 */
	public String getIP() {
		return getAddress().toString().substring(1);
	}
	
	/**
	 * Gets the port of this connection.
	 * 
	 * @return The port this connection is bound to.
	 */
	public int getPort() {
		return socket.getPort();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		
		if(obj == this) {
			return true;
		}
		
		if(obj instanceof Connection conn) {
			return socket.equals(conn.socket) && server.equals(conn.server);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Connection{socketStatus=" + !socket.isClosed() + ", ip=" + getIP() + ", port=" + getPort() + ", server=" + server + "}";
	}
	
	public String toStringWithoutServer() {
		return "Connection{socketStatus=" + !socket.isClosed() + ", ip=" + getIP() + ", port=" + getPort() + "}";
	}
}