package net.unknownuser.networking.game;

import java.io.*;

public class Tuple<X extends Serializable, Y extends Serializable> implements Serializable {
	private static final long serialVersionUID = -1169848846571112466L;
	
	public final X x;
  public final Y y;
  
  public Tuple(X obj1, Y obj2) { 
    this.x = obj1; 
    this.y = obj2; 
  } 
}