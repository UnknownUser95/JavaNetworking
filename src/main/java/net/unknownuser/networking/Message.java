package net.unknownuser.networking;

import java.io.*;

/**
 * A message which can be send and received by clients and servers.
 * @param <T> The type of message.
 * @param <C> The content type of the message.
 */
public class Message<T extends Enum<T>, C extends Serializable> implements Serializable {
	private static final long serialVersionUID = -3050435261069194088L;
	
	public final C content;
	public final T type;
	
	public Message(T type, C content) {
		super();
		this.type = type;
		this.content = content;
	}
}	
