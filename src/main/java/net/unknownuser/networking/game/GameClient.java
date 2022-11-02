package net.unknownuser.networking.game;

import java.io.*;
import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.List;
import org.eclipse.wb.swt.*;

import net.unknownuser.networking.*;

public class GameClient extends Client {
	
	protected GameClient(String ip, int port) {
		super(ip, port);
	}
	
	public static final int FIELD_SIZE = 50;
	public static final RGB BOARD_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB FIELD_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB FIELD_FOREGROUND = new RGB(0, 0, 0);
	
	private final Board board = new Board(20, 15);
	private int playerID = -1;
	
	private RGB playerColour = new RGB(255, 0, 0);
	
	protected Shell shell;
	private Text textChatInput;
	private List chatMessageList;
	private Canvas canvas;
	
	/**
	 * Launch the game.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		GameClient client = new GameClient("127.0.0.1", 50000);
		Tuple<Tuple<String, RGB>, Tuple<String, Integer>> playerPrefs = new PlayerCreator(new Shell(), SWT.NONE).open();
		
		Tuple<String, RGB> playerInfo = playerPrefs.x;
		Tuple<String, Integer> serverInfo = playerPrefs.y;
		
		// cancelled
		if(serverInfo.x == null) {
			System.out.println("connection canceled by user");
			return;
		}
		
		client.playerColour = playerInfo.y;
		
		client.setServerIP(serverInfo.x);
		client.setPort(serverInfo.y);
		
		try {
			client.connect();
			
			client.waitForID();
			
			client.sendMessage(new Message<>(MessageType.SET_PREFERENCES, playerInfo));
			
			client.open();
		} catch(IOException exc) {
			System.err.println("could not connect to server");
			System.err.println(exc.getMessage());
		}
	}
	
	private void waitForID() {
		synchronized (board) {
			if(playerID != -1) {
				System.out.println("ID is " + playerID);
				return;
			}
			
			System.out.println("waiting for ID...");
			while(playerID == -1) {
				try {
					board.wait();
				} catch(InterruptedException exc) {
					exc.printStackTrace();
				}
			}
			System.out.println("set ID to " + playerID);
		}
	}
	
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		// a weird text box appears without this
		shell.forceFocus();
		while(!shell.isDisposed() && isConnected()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if(isConnected()) {
			disconnect();
		}
	}
	
	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		waitForID();
		
		board.addPlayer(playerID, new Point(0, 0), playerColour);
		
		shell = new Shell();
		shell.setText("Multiplayer Game");
		
		// -------------------- main game --------------------
		
		// canvas as background and main game
		canvas = new Canvas(shell, SWT.NONE);
		canvas.setBounds(10, 10, board.width * FIELD_SIZE, board.height * FIELD_SIZE);
		canvas.setBackground(new Color(BOARD_BACKGROUND));
		
		// actually drawing the content
		canvas.addPaintListener(arg0 -> {
			GC gc = arg0.gc; // just a shorthand
			gc.setForeground(SWTResourceManager.getColor(FIELD_FOREGROUND));
			gc.setBackground(SWTResourceManager.getColor(FIELD_BACKGROUND));
			// draw empty field
			for(int x = 0; x < board.width; x++) {
				for(int y = 0; y < board.height; y++) {
					gc.fillRectangle(new Rectangle(x * FIELD_SIZE, y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
					gc.drawRectangle(new Rectangle(x * FIELD_SIZE, y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
				}
			}
			
			// draw players
			for(int id : board.getPlayerIDs()) {
				RGB colour = board.getPlayerColour(id);
				if(colour == null) {
					continue;
				}
				Point pos = board.getPlayerPosition(id);
				
				gc.setBackground(SWTResourceManager.getColor(colour));
				gc.fillRectangle(new Rectangle(pos.x * FIELD_SIZE, pos.y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
				gc.drawRectangle(new Rectangle(pos.x * FIELD_SIZE, pos.y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
			}
		});
		
		shell.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				// space moves to chat
				if(arg0.keyCode == SWT.SPACE) {
					textChatInput.forceFocus();
					return;
				}
				
				// movement input via WASD and arrow keys
				MoveDirection direction = switch(arg0.keyCode) {
				case 119, SWT.ARROW_UP -> MoveDirection.UP;
				case 97, SWT.ARROW_LEFT -> MoveDirection.LEFT;
				case 115, SWT.ARROW_DOWN -> MoveDirection.DOWN;
				case 100, SWT.ARROW_RIGHT -> MoveDirection.RIGHT;
				default -> MoveDirection.NONE;
				};
				// only update if required
				if(direction == MoveDirection.NONE) {
					// no move, other key is pressed
					return;
				}
				
				if(board.movePlayer(playerID, direction)) {
					sendMessage(new Message<>(MessageType.MOVE, direction));
					// redraw
					async(canvas::redraw);
				} else {
					// invalid move
					System.out.println("out of bounds move");
				}
			}
		});
		
		// clicking within the frame focuses the game
		shell.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				// setFocus() doesn't work
				shell.forceFocus();
			}
		});
		
		// -------------------- chat --------------------
		
		Group chatgroup = new Group(shell, SWT.NORMAL);
		chatgroup.setBounds(board.width * FIELD_SIZE + 20, 10, 450, canvas.getBounds().height);
		Rectangle groupBounds = chatgroup.getBounds();
		
		// displays received messages
		chatMessageList = new List(chatgroup, SWT.BORDER);
		chatMessageList.setBounds(10, 10, 430, groupBounds.height - 90);
		
		// text input
		textChatInput = new Text(chatgroup, SWT.BORDER);
		textChatInput.setBounds(10, groupBounds.height - 75, 430, 30);
		
		// send message
		// control + enter also sends a message
		Button btnSend = new Button(chatgroup, SWT.NONE);
		btnSend.setBounds(230, groupBounds.height - 40, 210, 30);
		btnSend.setText("Send");
		
		// clears all received messages
		Button btnClearChat = new Button(chatgroup, SWT.NONE);
		btnClearChat.setText("Clear Messages");
		btnClearChat.setBounds(10, groupBounds.height - 40, 210, 30);
		
		// pressing escape focuses the shell, allowing gameplay
		KeyAdapter focusOnESC = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.ESC) {
					shell.forceFocus();
				}
			}
		};
		textChatInput.addKeyListener(focusOnESC);
		btnClearChat.addKeyListener(focusOnESC);
		btnSend.addKeyListener(focusOnESC);
		
		// only send when control is held and enter is pressed
		textChatInput.addKeyListener(new KeyAdapter() {
			boolean controlDown = false;
			
			@Override
			public void keyReleased(KeyEvent arg0) {
				if(arg0.keyCode == SWT.CONTROL) {
					controlDown = false;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(controlDown && arg0.keyCode == 13) {
					sendChatMessageToServer(true);
				}
				if(arg0.keyCode == SWT.CONTROL) {
					controlDown = true;
				}
			}
		});
		
		// clear all messages
		btnClearChat.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				chatMessageList.removeAll();
			}
		});
		
		// send message
		btnSend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				sendChatMessageToServer(true);
			}
		});
		
		// -------------------- size of shell --------------------
		
		Point bounds = new Point(board.width * FIELD_SIZE + groupBounds.width + 30, groupBounds.height + 20);
		shell.setSize(bounds);
		shell.setMinimumSize(bounds);
		shell.setMaximumSize(bounds);
	}
	
	private void sendChatMessageToServer(boolean clearInput) {
		// send message
		// get content if needed
		String message = textChatInput.getText().strip();
		
		if(message.length() != 0) {
			sendMessage(new Message<>(MessageType.CHAT_MESSAGE, message));
		}
		if(clearInput) {
			// clear message
			textChatInput.setText("");
		}
	}
	
	public static void async(Runnable task) {
		Display.getDefault().asyncExec(task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onMessageReceived(Message<?, ?> message) {
		boolean redrawCanvas = false;
		switch((MessageType) message.type) {
		case CHAT_MESSAGE -> async(() -> chatMessageList.add((String) message.content));
		case SET_ID -> {
			playerID = (int) message.content;
			synchronized (board) {
				board.notifyAll();
			}
		}
		case MOVE -> {
			Tuple<Integer, MoveDirection> move = (Tuple<Integer, MoveDirection>) message.content;
			board.movePlayer(move.x, move.y);
			redrawCanvas = true;
		}
		case NEW_PLAYER -> {
			// add a new player
			// they don't have a colour yet
			Tuple<Integer, Point> newPlayer = (Tuple<Integer, Point>) message.content;
			board.addPlayer(newPlayer.x, newPlayer.y);
			redrawCanvas = true;
		}
		case SET_COLOUR -> {
			// change the colour of one player
			Tuple<Integer, RGB> idColour = (Tuple<Integer, RGB>) message.content;
			board.setPlayerColour(idColour.x, idColour.y);
			redrawCanvas = true;
		}
		case SYNC_PLAYERS -> {
			// update player location and colours
			ArrayList<Tuple<Integer, Tuple<Point, RGB>>> playerColours = (ArrayList<Tuple<Integer, Tuple<Point, RGB>>>) message.content;
			// addPlayer also updates the given player, if it already exists
			for(Tuple<Integer, Tuple<Point, RGB>> idColour : playerColours) {
				board.addPlayer(idColour.x, idColour.y.x, idColour.y.y);
			}
			redrawCanvas = true;
		}
		case DELETE_PLAYER -> {
			// remove a player from the game
			int id = (int) message.content;
			board.removePlayer(id);
			redrawCanvas = true;
		}
		default -> System.out.println("unknown or unhandled message type: " + message.type);
		}
		
		if(redrawCanvas) {
			async(canvas::redraw);
		}
	}
	
	@Override
	public void onConnect() {
		System.out.println("connected to server");
	}
	
	@Override
	public void onDisconnect(boolean withError) {
		System.out.printf("disconnected with%s error%n", (withError) ? "" : "out");
	}
}
