package net.unknownuser.networking.game;

import org.eclipse.swt.graphics.*;

public class Board {
	public final int width;
	public final int height;
	public final Field[][] fields;
	
	public Board(int width, int height) {
		super();
		this.width = width;
		this.height = height;
		fields = generateEmptyBoard(height, width);
	}

	public boolean swapFields(int x1, int y1, int x2, int y2) {
		if(!(isInbounds(new Point(x1, y1)) && isInbounds(new Point(x2, y2)))) {
			return false;
		}
		// get fields
		Field f1 = getField(x1, y1);
		Field f2 = getField(x2, y2);
		
		// assign in board position
		fields[x1][y1] = f2;
		fields[x2][y2] = f1;
		
		return true;
	}
	
	public Field getField(int x, int y) {
		return fields[x][y];
	}
	
	public void addField(Field newField, int x, int y) {
		fields[x][y] = newField;
	}
	
	public boolean moveField(int x, int y, MoveDirection direction) {
		return swapFields(x, y, x + direction.x, y + direction.y);
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
				board[x][y] = new Field(x, y);
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
