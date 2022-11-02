package net.unknownuser.networking.game;

import java.util.*;

import org.eclipse.swt.graphics.*;

/**
 * The board of the game.
 * Holds player id's, their position on the board, and their colours.
 */
public class Board {
	public final int width;
	public final int height;
	
	private final HashMap<Integer, Point> playerPosition = new HashMap<>();
	private final HashMap<Integer, RGB> playerColours = new HashMap<>();	
	
	public Board(int width, int height) {
		super();
		this.width = width;
		this.height = height;
	}
	
	public Map<Integer, Point> getPlayers() {
		return playerPosition;
	}
	
	public Set<Integer> getPlayerIDs() {
		return playerPosition.keySet();
	}
	
	public Point getPlayerPosition(int id) {
		return playerPosition.get(id);
	}
	
	public void setPlayerColour(int id, RGB colour) {
		playerColours.put(id, colour);
	}
	
	public RGB getPlayerColour(int id) {
		return playerColours.get(id);
	}
	
	public void addPlayer(int id, Point position) {
		playerPosition.put(id, position);
	}
	
	public void addPlayer(int id, Point position, RGB colour) {
		addPlayer(id, position);
		playerColours.put(id, colour);
	}
	
	public void removePlayer(int id) {
		playerColours.remove(id);
		playerPosition.remove(id);		
	}
	
	public boolean movePlayer(int playerID, MoveDirection direction) {
		if(direction == MoveDirection.NONE) {
			return true;
		}
		
		Point pos = playerPosition.get(playerID);
		// no position for player or out of bounds move
		if(pos == null || !isInbounds(new Point(pos.x + direction.x, pos.y + direction.y))) {
			return false;
		}
		
		// update location in map
		pos.x += direction.x;
		pos.y += direction.y;
		
		return true;
	}
	
	public boolean isInbounds(Point p) {
		return p.x >= 0 && p.y >= 0 && p.x < width && p.y < height;
	}

	@Override
	public String toString() {
		return "Board{width=" + width + ", height=" + height + ", players=" + getPlayerIDs().size() + ", playerIDs=" + getPlayerIDs() + "}";
	}
}
