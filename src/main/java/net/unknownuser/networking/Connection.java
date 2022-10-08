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
	
	@Override
	public void run() {
		while(!socket.isClosed()) {
			receiveMessage();
		}
	}
	
	private void receiveMessage() {
		try {
			Message<?, ?> message = (Message<?, ?>) socketReader.readObject();
			server.addMessageToQueue(new MessageToSend(message, this));
		} catch(IOException exc) {
			if(!(exc instanceof EOFException)) {
				System.out.println("error while reading message");
				exc.printStackTrace();
			}
			disconnect();
			server.removeConnection(this);
		} catch(ClassNotFoundException exc) {
			System.err.println("received object could not be mapped to a class");
		}
	}
	
	public void sendMessage(Message<?, ?> message) {
		try {
			socketWriter.writeObject(message);
			socketWriter.flush();
		} catch(IOException exc) {
			System.out.println("error while sending message");
			System.out.println(exc.getMessage());
			disconnect();
		}
	}
	
	public void disconnect() {
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
	
	public InetAddress getAddress() {
		return socket.getInetAddress();
	}
	
	public String getIP() {
		return getAddress().toString().substring(1);
	}
	
	public int getPort() {
		return socket.getLocalPort();
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
}