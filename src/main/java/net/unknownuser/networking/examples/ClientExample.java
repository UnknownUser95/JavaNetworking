package net.unknownuser.networking.examples;

import java.io.*;

import net.unknownuser.networking.*;

public class ClientExample extends Client {
	protected ClientExample(String ip, int port) {
		super(ip, port);
	}
	
	public static void main(String[] args) throws IOException {
		ClientExample client = new ClientExample("127.0.0.1", 50000);
		client.connect();
		
		System.out.print("input name: ");
		String input;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while(!(input = br.readLine()).equals("!exit") && client.isConnected()) {
				client.sendMessage(new Message<>(MessageType.TEXT, input));
			}
		} catch(IOException exc) {}
		client.disconnect();
	}

	@Override
	public void onMessageReceived(Message<?, ?> message) {
		System.out.println(message.content);
	}

	@Override
	public void onConnect() {
		System.out.println("connected");
	}

	@Override
	public void onDisconnect(boolean byError) {
		if(byError) {
			System.out.println("forced disconnect - server shut down");
		} else {
			System.out.println("disconnected with error");
		}
	}
}
