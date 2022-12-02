package application;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;

/**
 * Main class
 * 
 * @author aks60
 *
 */
public class App {
	
	public static final boolean DEBUG_MODE = true;
	
	public static int CLIENT_TICKRATE = 100;
	public static int COMMS_TICKRATE = 10;

	public static void sleepThread(String err, int tickrate) {
		try {
			Thread.sleep(tickrate);
		} catch (InterruptedException e) {
			System.out.println(err + " thread interrupted");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Connector connector = new Connector();
		
	    JFrame frame = new JFrame("Whiteboard App");
	    Container content = frame.getContentPane();
	    content.setLayout(new BorderLayout());

	    DrawingPanel panel = new DrawingPanel(connector);
	    content.add(panel, BorderLayout.CENTER);
	    
	    frame.setSize(800, 600);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);
	    
	    // Its important that this is called AFTER creating panel
		Thread connectorThread = new Thread(connector);
		connectorThread.start();
	    
	}

}
