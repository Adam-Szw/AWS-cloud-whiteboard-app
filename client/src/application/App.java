package application;

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
	
	public static String AUTOBALANCER_IP = "server-nlb-134ffeb68c27e8d3.elb.eu-west-2.amazonaws.com";
	public static int PORT = 80;

	public static void sleepThread(String err, int tickrate) {
		try {
			Thread.sleep(tickrate);
		} catch (InterruptedException e) {
			System.out.println(err + " thread interrupted");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Connector connector = new Connector(AUTOBALANCER_IP);
		Whiteboard whiteboard = new Whiteboard(connector);
		whiteboard.open(800, 600);

	    // Its important that this is called AFTER creating panel
		Thread connectorThread = new Thread(connector);
		connectorThread.start();
	    
	}

}
