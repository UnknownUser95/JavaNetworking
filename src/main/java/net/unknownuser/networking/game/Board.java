package net.unknownuser.networking.game;

import java.util.*;

import org.eclipse.swt.graphics.*;

public class Board {
	public final int width;
	public final int height;
	public final Field[][] fields;
	
	public final HashMap<Integer, Point> playerPosition = new HashMap<>();
	
	public Board(int width, int height) {
		super();
		this.width = width;
		this.height = height;
		fields = generateEmptyBoard(height, width);
	}

	public boolean swapFields(Point p1, Point p2) {
		if(!(isInbounds(p1) && isInbounds(p2))) {
			return false;
		}
		// get fields
		Field f1 = getField(p1.x, p1.y);
		Field f2 = getField(p2.x, p2.y);
		
		// assign in board position
		fields[p1.x][p1.y] = f2;
		fields[p2.x][p2.y] = f1;
		
		return true;
	}
	
	public Field getField(int x, int y) {
		return fields[x][y];
	}
	
	public Field getField(Point point) {
		return getField(point.x, point.y);
	}
	
	public void addPlayer(int id, Point position) {
		playerPosition.put(id, position);
	}
	
	public void removePlayer(int id) {
		
		getField(playerPosition.get(id)).setColour(255, 255, 255);
		
		playerPosition.remove(id);		
	}
	
	public void addField(Field newField, int x, int y) {
		fields[x][y] = newField;
	}
	
	public boolean moveField(Point p, MoveDirection direction) {
		return swapFields(p, new Point(p.x + direction.x, p.y + direction.y));
	}
	
	public boolean movePlayer(int playerID, MoveDirection direction) {
		Point pos = playerPosition.get(playerID);
		// no position for player or out of bounds move
		if(pos == null || !isInbounds(new Point(pos.x + direction.x, pos.y + direction.y))) {
			return false;
		}
		
		// swap fields
		swapFields(pos, new Point(pos.x + direction.x, pos.y + direction.y));
		
		// update location in map
		pos.x += direction.x;
		pos.y += direction.y;
		
		return true;
	}
	
	public static Field[][] generateEmptyBoard(int height, int width) {
		// width or height must be greater than 1
		// anything else wouldn't make sense
		if(height < 1 || width < 1) {
			return new Field[0][0];
		}
		
		Field[][] board = new Field[width][height];
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				board[x][y] = new Field();
			}
		}
		
//		System.out.println(board.length); // width
//		System.out.println(board[0].length); // height
		return board;
	}
	
	public boolean isInbounds(Point p) {
		return p.x >= 0 && p.y >= 0 && p.x < width && p.y < height;
	}

	@Override
	public String toString() {
		return "Board [width=" + width + ", height=" + height + "]";
	}
}
