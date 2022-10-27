package net.unknownuser.networking;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public abstract class Client {
	protected int port;
	protected String ip;
	
	protected Socket socket;
	protected ObjectOutputStream socketWriter;
	protected ObjectInputStream socketReader;
	
	protected Thread messageReceiver;
	protected Thread messageListener;
	
	protected final LinkedBlockingQueue<Message<?, ?>> receivedMessages = new LinkedBlockingQueue<>();
	
	protected Client(String ip, int port) {
		super();
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * Whenever a message is received from the server, this method is called with the received message.
	 * 
	 * @param message The received message.
	 */
	public abstract void onMessageReceived(Message<?, ?> message);
	/**
	 * When a successful connection has been established, this method is called.
	 */
	public abstract void onConnect();
	/**
	 * When the client has been disconnected from the server, this method is called.
	 * 
	 * @param withError Whether the disconnect was caused by an error.
	 */
	public abstract void onDisconnect(boolean withError);
	
	/**
	 * Connects the client to the specified IP and port.
	 * 
	 * @return {@code true} if a connection could be made, {@code false} otherwise.
	 * 
	 * @throws IOException Any exception during connecting.
	 */
	public boolean connect() throws IOException {
		if(isConnected()) {
			return false;
		}
		
		synchronized (this) {
			this.socket = new Socket(ip, port);
			
			this.socketWriter = new ObjectOutputStream(new DataOutputStream(socket.getOutputStream()));
			this.socketReader = new ObjectInputStream(new DataInputStream(socket.getInputStream()));
			
			messageReceiver = new Thread(this::receiveMessage, "messageReceiver");
			messageReceiver.start();
			
			messageListener = new Thread(this::waitForMessages, "messageListener");
			messageListener.setDaemon(true);
			messageListener.start();
			
			onConnect();
		}
		return true;
	}
	
	/**
	 * Disconnects the client from the server.
	 * 
	 * @param byError Whether the disconnect is caused by an error.
	 * 
	 * @return {@code true} if a disconnect was possible, {@code false} otherwise.
	 */
	protected synchronized boolean disconnect(boolean byError) {
		if(!isConnected()) {
			return false;
		}
		
		try {
			synchronized (this) {
				synchronized (socket) {
					// stop threads
					messageReceiver.interrupt();
					messageListener.interrupt();
					
					socket.close();
					
					if(socketWriter != null) {
						socketWriter.close();
					}
					if(socketReader != null) {
						socketReader.close();
					}
					onDisconnect(byError);
				}
			}
		} catch(IOException exc) {
			System.err.println("error during disconnect");
			exc.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Disconnects the client.
	 */
	public void disconnect() {
		disconnect(false);
	}
	
	/**
	 * Sends a message to the connected server.
	 * 
	 * @param message The message to send.
	 * 
	 * @return {@code true} if the message could be send, {@code false} otherwise.
	 */
	public boolean sendMessage(Message<?, ?> message) {
		if(!isConnected()) {
			return false;
		}
		
		try {
			// socket must be under control
			synchronized (socket) {
				
				socketWriter.writeObject(message);
				socketWriter.flush();
			}
		} catch(IOException exc) {
			exc.printStackTrace();
			disconnect(true);
			return false;
		}
		return true;
	}
	
	/**
	 * Receives a message from the server. onMessageReceived} with it.
	 */
	private void receiveMessage() {
		boolean isError = false;
		try {
			while(isConnected()) {
				try {
					Message<?, ?> message = (Message<?, ?>) socketReader.readObject();
					// keep this thread listening.
					addMessageToQueue(message);
				} catch(ClassNotFoundException exc) {
					System.err.println("received object could not be mapped to a class");
				}
			}
		} catch(EOFException exc) {
			// socket closed during read
			// no actual error
		} catch(SocketException exc) {
			// socket closed manually or through server
			// no actual error
			if(!exc.getMessage().equals("Socket closed")) {
				isError = true;
			}
		} catch(IOException exc) {
			// general errors
			exc.printStackTrace();
			isError = true;
		} finally {
			disconnect(isError);
		}
	}
	
	/**
	 * Waits for messages to appear and and calls {@link #onMessageReceived(Message) on them.
	 */
	private void waitForMessages() {
		try {
			while(isConnected()) {
				Message<?, ?> message = receivedMessages.take();
				onMessageReceived(message);
			}
		} catch(InterruptedException exc) {
			// thrown on disconnect
		}
	}
	
	/**
	 * Adds a message to the message list, which will be sent to the server, instead of directly sending it.
	 * 
	 * @param newMessage The message to send.
	 * 
	 * @return {@code true} if the message could be added, {@code false} otherwise.
	 */
	public boolean addMessageToQueue(Message<?, ?> newMessage) {
		return receivedMessages.offer(newMessage);
	}
	
	/**
	 * Returns whether this client is connected to a server,
	 * 
	 * @return {@code true} if a connection is made, {@code false} otherwise.
	 */
	public boolean isConnected() {
		return socket != null && !socket.isClosed();
	}
	
	/**
	 * Gets the IP of the server this client uses.
	 * 
	 * @return The server's IP as a string.
	 */
	public String getServerIP() {
		return ip;
	}
	
	/**
	 * Changes the server this client uses to the given one. This only has an effect, if the client isn't already connected to a server.
	 * 
	 * @param newIP The IP of the new server.
	 * 
	 * @return {@code true} if the servers' IP could be changed, {@code false} otherwise.
	 */
	public boolean setServerIP(String newIP) {
		// can only be changed when entire client is controlled
		synchronized (this) {
			if(socket.isClosed()) {
				ip = newIP;
				return true;
			} else {
				System.out.println("could not change IP, client is connected");
				return false;
			}
		}
	}
	
	/**
	 * Gets the port this client uses.
	 * 
	 * @return The used port.
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Changes the port this client uses to the given one. This only has an effect, if the client isn't already connected to a server.
	 * 
	 * @param newPort The port of the new server.
	 * 
	 * @return {@code true} if the port could be changed, {@code false} otherwise.
	 */
	public boolean setPort(int newPort) {
		// can only be changed when entire client is controlled
		synchronized (this) {
			if(socket.isClosed()) {
				port = newPort;
				return true;
			} else {
				System.out.println("could not change port, client is connected");
				return false;
			}
		}
	}
}
