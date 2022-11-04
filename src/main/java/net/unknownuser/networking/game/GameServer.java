package net.unknownuser.networking.game;

import java.io.*;
import java.util.*;

import org.eclipse.swt.graphics.*;

import net.unknownuser.networking.*;

public class GameServer extends Server {
	private final HashMap<Connection, Integer> connectionIDs = new HashMap<>();
	private final HashMap<Integer, String> playerNames = new HashMap<>();
	private int idIndex = 0;
	
	private static final Point DEFAULT_STARTING_POSITION = new Point(0, 0);
	
	private Board board = new Board(20, 15);
	
	protected GameServer(int port) {
		super(port);
	}
	
	public static void main(String[] args) throws IOException {
		GameServer server = new GameServer(50000);
		server.start();
	}
	
	public Point getDefaultStartingPoint() {
		return new Point(DEFAULT_STARTING_POSITION.x, DEFAULT_STARTING_POSITION.y);
	}
	
	public void synchronizePlayers() {
		// synchronize existing players
		if(connectionIDs.size() > 1) {
			// synchronize already connected players
			ArrayList<Tuple<Integer, Tuple<Point, RGB>>> colours = new ArrayList<>();
			for(int id : connectionIDs.values()) {
//				colours.add(new Tuple<>(id, playerSettings.get(id).y));
				Point pos = board.getPlayerPosition(id);
				RGB col = board.getPlayerColour(id);
				if(pos == null || col == null) {
					System.out.println("conf null: ");
					System.out.println("id: " + id);
					System.out.println("colour: " + col);
					System.out.println("position: " + pos);
					continue;
				}
				colours.add(new Tuple<>(id, new Tuple<>(pos, col)));
			}
			if(!colours.isEmpty()) {
				broadcastMessage(new MessageToSend(new Message<>(MessageType.SYNC_PLAYERS, colours), null));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMessageReceived(Message<?, ?> message, Connection sender) {
		int senderID = -1;
		synchronized (connectionIDs) {
			senderID = connectionIDs.get(sender);
		}
		switch((MessageType) message.type) {
		case CHAT_MESSAGE -> {
			String chatMessage = String.format("%s: %s%n", playerNames.get(senderID), message.content);
			broadcastMessage(new MessageToSend(new Message<>(MessageType.CHAT_MESSAGE, chatMessage), sender));
		}
		case SET_PREFERENCES -> {
			Tuple<String, RGB> playerPrefs = (Tuple<String, RGB>) message.content;
			board.setPlayerColour(senderID, playerPrefs.y);
			if(playerPrefs.x.isBlank()) {
				System.out.println("anonymous user connected");
				playerNames.put(senderID, "Anon");
			} else {
				playerNames.put(senderID, playerPrefs.x);
			}
			
			synchronizePlayers();
		}
		case MOVE -> {
			MoveDirection dir = (MoveDirection) message.content;
			if(board.movePlayer(senderID, dir)) {
				broadcastMessage(new MessageToSend(new Message<>(MessageType.MOVE, new Tuple<>(senderID, dir)), sender));
			} else {
				sender.sendMessage(new Message<>(MessageType.MOVE_REJECTED, dir));
			}
		}
		default -> System.out.println("unkown or unhandled message type " + message.type);
		}
	}
	
	@Override
	public void onClientConnected(Connection client) {
		synchronized (connectionIDs) {
			connectionIDs.put(client, ++idIndex);
			Point pos = getDefaultStartingPoint();
			board.addPlayer(idIndex, pos);
			
			// give ID to new player
			client.sendMessage(new Message<>(MessageType.SET_ID, idIndex));

			// notify all other players of new player
			broadcastMessage(new MessageToSend(new Message<>(MessageType.NEW_PLAYER, new Tuple<>(idIndex, pos)), client));
			
			System.out.println("client connected");
		}
	}
	
	@Override
	public void onClientDisconnected(Connection client) {
		int senderID = -1;
		synchronized (connectionIDs) {
			senderID = connectionIDs.get(client);
		}
		
		broadcastMessage(new MessageToSend(new Message<>(MessageType.DELETE_PLAYER, senderID), client));
		
		// remove player from maps
		playerNames.remove(senderID);
		connectionIDs.remove(client);
		
		System.out.println("client disconnected");
	}
	
}
