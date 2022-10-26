package net.unknownuser.networking.game;

public enum MoveDirection {
	// numbers adapted to match board direction
	// 0-0 is top left, not bottom left
	UP(0, -1),
	RIGHT(1, 0),
	DOWN(0, 1),
	LEFT(-1, 0),
	NONE(0, 0);
	
	public final int x;
	public final int y;
	
	private MoveDirection(int x, int y) {
		this.x = x;
		this.y = y;
	}
}