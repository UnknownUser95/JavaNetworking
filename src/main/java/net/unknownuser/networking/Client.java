package net.unknownuser.networking;

import java.io.*;
import java.net.*;

public abstract class Client {
	protected final String ip;
	protected final int port;
	protected Socket socket;
	protected ObjectOutputStream socketWriter;
	protected ObjectInputStream socketReader;
	
	protected Client(String ip, int port) {
		super();
		this.ip = ip;
		this.port = port;
	}
	
	public abstract void onMessageReceived(Message<?, ?> message);
	public abstract void onConnect();
	public abstract void onDisconnect(boolean withError);
	
	public void connect() throws IOException {
			this.socket = new Socket(ip, port);
			this.socketWriter = new ObjectOutputStream(new DataOutputStream(socket.getOutputStream()));
			this.socketReader = new ObjectInputStream(new DataInputStream(socket.getInputStream()));
			Thread messageReceiver = new Thread(this::receiveMessage);
			messageReceiver.setDaemon(true);
			messageReceiver.start();
			onConnect();
	}
	
	public void sendMessage(Message<?, ?> message) {
		try {
			if(socket.isClosed()) {
				return;
			}
			
			socketWriter.writeObject(message);
			socketWriter.flush();
		} catch(IOException exc) {
			exc.printStackTrace();
			disconnect(true);
		}
	}
	
	protected void receiveMessage() {
		try {
			while(!socket.isClosed()) {
				Message<?, ?> message = (Message<?, ?>) socketReader.readObject();
				onMessageReceived(message);
			}
		} catch(IOException exc) {
			disconnect(true);
		} catch(ClassNotFoundException exc) {
			System.err.println("received object could not be mapped to a class");
		}
	}
	
	private void disconnect(boolean byError) {
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
			onDisconnect(byError);
		} catch(IOException exc) {
			System.out.println("error while closing");
		}
	}
	
	public void disconnect() {
		disconnect(false);
	}
}
