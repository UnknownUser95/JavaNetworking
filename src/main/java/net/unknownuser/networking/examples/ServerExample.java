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
			ServerExample server = new ServerExample(50000);
			server.start();
		} catch(IOException exc) {
			System.out.println("couldn't instanciate server");
			System.err.println(exc.getMessage());
		}
	}
	
	@Override
	public void onMessageReceived(MessageToSend message) {
		if(names.get(message.sender) == null) {
			String newName = (String) message.message.content;
			names.put(message.sender, newName);
			System.out.printf("assigned name %s to %s%n", newName, message.sender);
			return;
		}
		
		String sendingMessage = String.format("%s: %s", names.get(message.sender), message.message.content);
		
		System.out.println(sendingMessage);
		
		broadcastMessage(new MessageToSend(new Message<>(MessageType.TEXT, sendingMessage), message.sender));
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
