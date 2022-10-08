package net.unknownuser.networking;

public class MessageToSend {
	public final Message<?, ?> message;
	public final Connection sender;
	
	public MessageToSend(Message<?, ?> message, Connection sender) {
		super();
		this.message = message;
		this.sender = sender;
	}
}
