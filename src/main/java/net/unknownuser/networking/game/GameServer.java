package net.unknownuser.networking.game;

import java.io.*;
import java.util.*;

import net.unknownuser.networking.*;

public class GameServer extends Server {
	private final HashMap<Connection, Integer> connectionIDs = new HashMap<>();
//	private final HashMap<Integer, Point> playerPositions = new HashMap<>();
	private final HashMap<Integer, String> playerNames = new HashMap<>();
	private int idIndex = 1; // skip 0
	
	protected GameServer(int port) {
		super(port);
	}
	
	public static void main(String[] args) throws IOException {
		GameServer server = new GameServer(50000);
		server.start();
	}
	
	@Override
	public void onMessageReceived(Message<?, ?> message, Connection sender) {
		int senderID = connectionIDs.get(sender);
		switch((MessageType) message.type) {
		case CHAT_MESSAGE -> {
			System.out.println("broadcasting message");
			String chatMessage = String.format("%s: %s%n", playerNames.get(senderID), message.content);
			broadcastMessage(new MessageToSend(new Message<>(MessageType.CHAT_MESSAGE, chatMessage), sender));
		}
		case REQUEST_ID -> {
			synchronized (connectionIDs) {
				int id = idIndex++;
				connectionIDs.put(sender, id);
				sender.sendMessage(new Message<>(MessageType.REQUEST_ID, id));
			}
		}
		default -> System.out.println("unkown or unhandled message type " + message.type);
		}
	}
	
	@Override
	public void onClientConnected(Connection client) {
		connectionIDs.put(client, -1);
		System.out.println("client connected");
	}
	
	@Override
	public void onClientDisconnected(Connection client) {
		System.out.println("client disconnected");
	}
	
}
