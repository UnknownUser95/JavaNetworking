package net.unknownuser.networking.game;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import net.unknownuser.networking.*;

public class GameClient extends Client {
	
	protected GameClient(String ip, int port) {
		super(ip, port);
	}

	public static final int FIELD_SIZE = 50;
	public static final RGB BOARD_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB FIELD_BACKGROUND = new RGB(255, 255, 255);
	public static final RGB FIELD_FOREGROUND = new RGB(0, 0, 0);
	
	private static Board board = new Board(20, 10);
	private static Field playerField = new Field(0, 0);
	private static Point playerPosition = new Point(0, 0);
	
	protected Shell shell;
	
	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			GameClient window = new GameClient("127.0.0.1", 50000);
			window.open();
		} catch(Exception e) {
			e.printStackTrace();
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
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		board.addField(playerField, playerPosition.x, playerPosition.y);
		playerField.setColour(255, 0, 0);
		
		shell = new Shell();
		shell.setSize(board.width * FIELD_SIZE + 20, board.height * FIELD_SIZE + 20);
		shell.setText("Multiplayer Game");
		Rectangle shellBounds = shell.getBounds();
		shell.setMinimumSize(shellBounds.width, shellBounds.height);
		shell.setMaximumSize(shellBounds.width, shellBounds.height);
		
		Canvas canvas = new Canvas(shell, SWT.NONE);
		canvas.setBounds(10, 10, board.width * FIELD_SIZE, board.height * FIELD_SIZE);
		canvas.setBackground(new Color(BOARD_BACKGROUND));
		
		canvas.addPaintListener(arg0 -> {
			GC gc = arg0.gc;
//			Consumer<Field> drawField = field -> {
//				gc.setBackground(new Color(field.getColour()));
//				gc.setForeground(new Color(FIELD_FOREGROUND));
//				gc.fillRectangle(new Rectangle(field.x * FIELD_SIZE, field.y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
//				gc.drawRectangle(new Rectangle(field.x * FIELD_SIZE, field.y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
//				gc.drawString(field.getSymbol(), field.x * FIELD_SIZE + 20, field.y * FIELD_SIZE + 16);
//			};
		
//			for(Field[] fArr : board.fields) {
//				for(Field field : fArr) {
//					drawField.accept(field);
//				}
//			}
			for(int x = 0; x < board.width; x++) {
				for(int y = 0; y < board.height; y++) {
					Field field = board.getField(x, y);
					
					gc.setBackground(new Color(field.getColour()));
					gc.setForeground(new Color(FIELD_FOREGROUND));
					gc.fillRectangle(new Rectangle(x * FIELD_SIZE, y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
					gc.drawRectangle(new Rectangle(x * FIELD_SIZE, y * FIELD_SIZE, FIELD_SIZE, FIELD_SIZE));
					gc.drawString(field.getSymbol(), x * FIELD_SIZE + 20, y * FIELD_SIZE + 16);
				}
			}
		});
		
		shell.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				// not required
				// we only care about when keys are pressed
			}
			
			@Override
			public void keyPressed(KeyEvent arg0) {
				// allow WASD and arrow keys
				MoveDirection direction = switch(arg0.keyCode) {
				case 119, SWT.ARROW_UP -> MoveDirection.UP;
				case 97, SWT.ARROW_LEFT -> MoveDirection.LEFT;
				case 115, SWT.ARROW_DOWN -> MoveDirection.DOWN;
				case 100, SWT.ARROW_RIGHT -> MoveDirection.RIGHT;
				default -> MoveDirection.NONE;
				};
				if(board.moveField(playerPosition.x, playerPosition.y, direction)) {
					// update player position
					playerPosition.x += direction.x;
					playerPosition.y += direction.y;
					// redraw
					async(canvas::redraw);
				} else {
					System.out.println("out of bounds move");
				}
			}
		});
	}
	
	public static void async(Runnable task) {
		Display.getDefault().asyncExec(task);
	}

	@Override
	public void onMessageReceived(Message<?, ?> message) {}

	@Override
	public void onConnect() {}

	@Override
	public void onDisconnect(boolean withError) {}
}
