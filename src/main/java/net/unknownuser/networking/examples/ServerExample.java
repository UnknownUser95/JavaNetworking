package net.unknownuser.networking.examples;

import java.io.*;
import java.util.*;

import net.unknownuser.networking.*;

/**
 * This is a server for a chat room.
 */
public class ServerExample extends Server {	
	public ServerExample(int port) throws IOException {
		super(port);
	}
	
	private final HashMap<Connection, String> names = new HashMap<>();
	
	public static void main(String[] args) {
		try {
			// create the server on port 50000
			ServerExample server = new ServerExample(50000);
			// and start it
			server.start();
			// server stays up for 60 seconds
			try {
				Thread.sleep(60000);
			} catch(InterruptedException ignore) {}
			// shut down server
			server.shutdown();
			
		} catch(IOException exc) {
			System.out.println("couldn't instanciate server");
			System.err.println(exc.getMessage());
		}
	}
	
	@Override
	public void onMessageReceived(Message<?, ?> message, Connection sender) {
		if(names.get(sender) == null) {
			String newName = (String) message.content;
			names.put(sender, newName);
			System.out.printf("assigned name %s to %s%n", newName, sender.toStringWithoutServer());
			broadcastMessage(new MessageToSend(new Message<>(MessageType.TEXT, newName + " has joined"), sender));
			return;
		}
		
		String sendingMessage = String.format("%s: %s", names.get(sender), message.content);
		
		System.out.println(sendingMessage);
		
		broadcastMessage(new MessageToSend(new Message<>(MessageType.TEXT, sendingMessage), sender));
	}

	@Override
	public void onClientConnected(Connection client) {
		names.put(client, null);
		System.out.printf("client (%s:%d) connected%n", client.getIP(), client.getPort());
	}

	@Override
	public void onClientDisconnected(Connection client) {
		names.remove(client);
		System.out.printf("client (%s:%d) disconnected%n", client.getIP(), client.getPort());
	}
}
