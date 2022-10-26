package net.unknownuser.networking.game;

import net.unknownuser.networking.*;

public class GameServer extends Server {
	
	protected GameServer(int port) {
		super(port);
	}
	
	public static void main(String[] args) {
		
	}

	@Override
	public void onMessageReceived(Message<?, ?> message, Connection sender) {
		switch((MessageType) message.type) {
		case BOARD_SYNC -> {}
		case CHAT_MESSAGE -> {}
		case MOVE -> {}
		}
	}
	
	@Override
	public void onClientConnected(Connection client) {}
	
	@Override
	public void onClientDisconnected(Connection client) {}
	
}
