package net.unknownuser.networking.game;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.GridData;

public class PlayerCreator extends Dialog {
	public static final String DEFAULT_NAME = "";
	public static final RGB DEFAUL_COLOUR = new RGB(0, 0, 0);
	public static final String DEFAULT_SERVER_IP = "127.0.0.1";
	public static final int DEFAULT_SERVER_PORT = 50000;
	
	private String name = DEFAULT_NAME;
	private RGB colour = DEFAUL_COLOUR;
	private String serverIP = DEFAULT_SERVER_IP;
	private int serverPort = DEFAULT_SERVER_PORT;
	
	protected Shell shell;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public PlayerCreator(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Tuple<Tuple<String, RGB>, Tuple<String, Integer>> open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return new Tuple<>(new Tuple<>(name, colour), new Tuple<>(serverIP, serverPort));
	}
	
	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(450, 320);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		
		// -------------------- player infos --------------------
		
		Group playerInfoGroup = new Group(shell, SWT.NONE);
		FormData formDataGroup1 = new FormData();
		formDataGroup1.top = new FormAttachment(0, 10);
		formDataGroup1.left = new FormAttachment(0, 10);
		formDataGroup1.bottom = new FormAttachment(0, 170);
		formDataGroup1.right = new FormAttachment(0, 440);
		playerInfoGroup.setLayoutData(formDataGroup1);
		playerInfoGroup.setLayout(new GridLayout(3, false));
		
		Label lblName = new Label(playerInfoGroup, SWT.NONE);
		lblName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblName.setText("Name: ");
		lblName.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		
		Text textName = new Text(playerInfoGroup, SWT.BORDER);
		textName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		textName.setToolTipText("player name");
		
		Label lblRed = new Label(playerInfoGroup, SWT.NONE);
		lblRed.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblRed.setText("Blue:");
		lblRed.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		
		Spinner spinnerRed = new Spinner(playerInfoGroup, SWT.BORDER);
		spinnerRed.setIncrement(5);
		spinnerRed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		spinnerRed.setMaximum(255);
		
		Canvas canvas = new Canvas(playerInfoGroup, SWT.BORDER);
		GridData canvasGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		canvasGridData.heightHint = 90;
		canvas.setLayoutData(canvasGridData);
		canvas.setBackground(SWTResourceManager.getColor(0, 0, 0));
		
		Label lblGreen = new Label(playerInfoGroup, SWT.NONE);
		lblGreen.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblGreen.setText("Green:");
		lblGreen.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		
		Spinner spinnerGreen = new Spinner(playerInfoGroup, SWT.BORDER);
		spinnerGreen.setIncrement(5);
		spinnerGreen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		spinnerGreen.setMaximum(255);
		
		Label lblBlue = new Label(playerInfoGroup, SWT.NONE);
		lblBlue.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblBlue.setText("Red:");
		lblBlue.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		
		Spinner spinnerBlue = new Spinner(playerInfoGroup, SWT.BORDER);
		spinnerBlue.setIncrement(5);
		spinnerBlue.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		spinnerBlue.setMaximum(255);
		
		textName.addModifyListener(arg0 -> name = textName.getText());
		
		ModifyListener applyColour = arg0 -> {
			colour = new RGB(spinnerRed.getSelection(), spinnerGreen.getSelection(), spinnerBlue.getSelection());
			async(() -> canvas.setBackground(new Color(colour)));
		};
		spinnerBlue.addModifyListener(applyColour);
		spinnerRed.addModifyListener(applyColour);
		spinnerGreen.addModifyListener(applyColour);
		
		switch(new Random().nextInt(3)) {
		case 0 -> spinnerRed.setSelection(255);
		case 1 -> spinnerGreen.setSelection(255);
		case 2 -> spinnerBlue.setSelection(255);
		}
		
		// -------------------- server infos --------------------
		
		Group serverInfoGroup = new Group(shell, SWT.NONE);
		serverInfoGroup.setLayout(new GridLayout(2, false));
		FormData formDataGroup2 = new FormData();
		formDataGroup2.bottom = new FormAttachment(100, -50);
		formDataGroup2.right = new FormAttachment(playerInfoGroup, 0, SWT.RIGHT);
		formDataGroup2.top = new FormAttachment(playerInfoGroup, 10);
		formDataGroup2.left = new FormAttachment(playerInfoGroup, 0, SWT.LEFT);
		serverInfoGroup.setLayoutData(formDataGroup2);
		
		Label lblIP = new Label(serverInfoGroup, SWT.NONE);
		lblIP.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		lblIP.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblIP.setText("IP:");
		
		Text textIP = new Text(serverInfoGroup, SWT.BORDER);
		textIP.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		textIP.setText(serverIP);
		
		Label lblPort = new Label(serverInfoGroup, SWT.NONE);
		lblPort.setFont(SWTResourceManager.getFont("Noto Sans", 15, SWT.NORMAL));
		lblPort.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2));
		lblPort.setText("Port:");
		
		Spinner spinnerPort = new Spinner(serverInfoGroup, SWT.BORDER);
		spinnerPort.setMinimum(1);
		spinnerPort.setMaximum(65535);
		spinnerPort.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
		spinnerPort.setSelection(serverPort);
		
		Button btnConnect = new Button(shell, SWT.NONE);
		FormData btnConnectFormData = new FormData();
		btnConnectFormData.bottom = new FormAttachment(100, -10);
		btnConnectFormData.left = new FormAttachment(playerInfoGroup, 0, SWT.LEFT);
		btnConnectFormData.width = 210;
		btnConnect.setLayoutData(btnConnectFormData);
		btnConnect.setText("Connect");
		
		Button btnCancel = new Button(shell, SWT.NONE);
		FormData btnCancelFormData = new FormData();
		btnCancelFormData.bottom = new FormAttachment(100, -10);
		btnCancelFormData.right = new FormAttachment(playerInfoGroup, 0, SWT.RIGHT);
		btnCancelFormData.width = 210;
		btnCancel.setLayoutData(btnCancelFormData);
		btnCancel.setText("Cancel");
		
		btnConnect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
			}
		});
		
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				serverIP = null;
				shell.dispose();
			}
		});
		
		textIP.addModifyListener(arg0 -> serverIP = textIP.getText());
		spinnerPort.addModifyListener(arg0 -> serverPort = spinnerPort.getSelection());
	}
	
	private void async(Runnable task) {
		Display.getDefault().asyncExec(task);
	}
}
