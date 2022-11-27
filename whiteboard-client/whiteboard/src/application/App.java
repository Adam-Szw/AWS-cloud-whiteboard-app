package application;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;

import application.drawings.Line;

public class App {
	
	public static String currentState = "";
	public static boolean stateChanged = false;
	
	public static void main(String[] args) {
		// this is where we decide which server to connect to
		// based on servers load
	    Comms comms = new Comms("localhost", 6668);
	    Thread commThread = new Thread(comms);
		
	    JFrame frame = new JFrame("Whiteboard App");
	    Container content = frame.getContentPane();
	    content.setLayout(new BorderLayout());
	    
	    State state = new State(comms);

	    DrawingPanel panel = new DrawingPanel(state);
	    content.add(panel, BorderLayout.CENTER);
	    
	    frame.setSize(800, 600);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);
	    
	    commThread.start();
	    
		Thread implementerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						if(stateChanged) {
							panel.clear();
							System.out.println("updating board to: " + currentState);
							for(String str : currentState.split(";")) {
								System.out.println("drawing: " + str);
								if(str.length() >= 4 && str.substring(0, 4).equals("Line")) {
									panel.implementer.draw(new Line(str));
								}
							}
							stateChanged = false;
						} else Thread.sleep(10);
					} catch(Exception e){ System.out.println(e); }
				}
			}
		});
		implementerThread.start();
	}

}
