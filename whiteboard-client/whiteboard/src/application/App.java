package application;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;

import application.drawings.Line;

public class App {
	
	public static void main(String[] args) {
		// this is where we decide which server to connect to
		// based on servers load
	    Comms comms = new Comms("localhost", 6668);
	    Thread commThread = new Thread(comms);
		
	    JFrame frame = new JFrame("Whiteboard App");
	    Container content = frame.getContentPane();
	    content.setLayout(new BorderLayout());

	    DrawingPanel panel = new DrawingPanel(comms);
	    content.add(panel, BorderLayout.CENTER);
	    
	    frame.setSize(800, 600);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);
	    
	    commThread.start();
	}

}
