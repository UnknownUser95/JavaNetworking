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
	
//	@Override
//	public synchronized boolean start() throws IOException {
//		boolean started = super.start();
//		// automatically synchronize player positions
//		if(started) {
//			// every 5 seconds, synchronize all players
//			Timer timer = new Timer();
//			TimerTask task = new TimerTask() {
//				@Override
//				public void run() {
//					synchronizePlayerPositions();
//				}
//			};
//			timer.schedule(task, 0, 5 * 1000);
//		}
//		return started;
//	}
	
	public Point getDefaultStartingPoint() {
		return new Point(DEFAULT_STARTING_POSITION.x, DEFAULT_STARTING_POSITION.y);
	}
	
	public void synchronizePlayerPositions() {
		if(connectionIDs.size() > 1) {
			synchronized (connectionIDs) {
				ArrayList<Tuple<Integer, Point>> positions = new ArrayList<>(connectionIDs.size());
				for(int id : connectionIDs.values()) {
					Point pos = board.getPlayerPosition(id);
					if(pos == null) {
						System.out.println("config null:");
						System.out.println("id: " + id);
						System.out.println("pos:" + pos);
						continue;
					}
					
					positions.add(new Tuple<>(id, pos));
				}
				
				if(!positions.isEmpty()) {
					broadcastMessage(new MessageToSend(new Message<>(MessageType.SYNC_PLAYER_POSITIONS, positions), null));
				}
			}
		}
	}
	
	public void synchronizePlayers() {
		// synchronize existing players
		if(connectionIDs.size() > 1) {
			synchronized (connectionIDs) {
				// synchronize already connected players
				ArrayList<Tuple<Integer, Tuple<Point, RGB>>> config = new ArrayList<>(connectionIDs.size());
				for(int id : connectionIDs.values()) {
					Point pos = board.getPlayerPosition(id);
					RGB col = board.getPlayerColour(id);
					if(pos == null || col == null) {
						System.out.println("config null:");
						System.out.println("id: " + id);
						System.out.println("colour: " + col);
						System.out.println("position: " + pos);
						continue;
					}
					config.add(new Tuple<>(id, new Tuple<>(pos, col)));
				}
				
				if(!config.isEmpty()) {
					broadcastMessage(new MessageToSend(new Message<>(MessageType.SYNC_PLAYERS, config), null));
				}
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
		case REQUEST_POSITION -> {
			sender.sendMessage(new Message<>(MessageType.SET_POSITION, new Tuple<>(senderID, board.getPlayerPosition(senderID))));
		}
		case SET_PREFERENCES -> {
			Tuple<String, RGB> playerPrefs = (Tuple<String, RGB>) message.content;
			synchronized (board) {
				board.setPlayerColour(senderID, playerPrefs.y);
			}
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
			boolean successfulMove = false;
			
			synchronized (board) {
				successfulMove = board.movePlayer(senderID, dir);
			}
			
			if(successfulMove) {
				broadcastMessage(new MessageToSend(new Message<>(MessageType.MOVE, new Tuple<>(senderID, dir)), null));
			} else {
				sender.sendMessage(new Message<>(MessageType.MOVE_REJECTED, 0));
			}
		}
		default -> System.out.println("unkown or unhandled message type " + message.type);
		}
	}
	
	@Override
	public void onClientConnected(Connection client) {
		Point pos;
		synchronized (connectionIDs) {
			synchronized (board) {
				connectionIDs.put(client, ++idIndex);
				pos = getDefaultStartingPoint();
				board.addPlayer(idIndex, pos);
			}
		}
		
		// give ID to new player
		client.sendMessage(new Message<>(MessageType.SET_ID, idIndex));
		
		// notify all other players of new player
		broadcastMessage(new MessageToSend(new Message<>(MessageType.NEW_PLAYER, new Tuple<>(idIndex, pos)), client));
		
		System.out.println("client connected");
	}
	
	@Override
	public void onClientDisconnected(Connection client) {
		int senderID = -1;
		synchronized (connectionIDs) {
			senderID = connectionIDs.get(client);
			
			broadcastMessage(new MessageToSend(new Message<>(MessageType.DELETE_PLAYER, senderID), client));
			
			// remove player from maps
			synchronized (board) {
				board.removePlayer(senderID);
			}
			playerNames.remove(senderID);
			connectionIDs.remove(client);
		}
		
		System.out.println("client disconnected");
	}
	
}
