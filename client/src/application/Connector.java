package application;

/**
 * This class is responsible for maintaining connection to the server.
 * It detects that connection was dropped and re-tries to connect to the server
 * 
 * @author aks60
 *
 */
public class Connector implements Runnable {

	public boolean closed = false;
	public Comms comms;	// Currently available comms. Inactive comms are still present when disconnected from server
	
	public boolean newComms = true;
	
	private int waitTime = App.CLIENT_TICKRATE;
	private int maxWaitTime = App.CLIENT_TICKRATE * 100;
	
	private String serverIP;
	
	public Connector(String serverIP) {
		this.serverIP = serverIP;
		comms = new Comms(serverIP, App.PORT);
	}
	
	@Override
	public void run() {
		while(!closed) {
		    Thread commThread = new Thread(comms);
		    commThread.start();
		    newComms = true;
		    // Wait for connection to be made
		    while(!comms.init) App.sleepThread("Connector thread", App.CLIENT_TICKRATE);
		    // Periodically check if comms are working
		    while(!comms.closed) App.sleepThread("Connector thread", App.CLIENT_TICKRATE);
		    if(App.DEBUG_MODE) System.out.println("Server connection lost. Retrying to connect...");
		    App.sleepThread("Connector thread", waitTime);
		    waitTime = Math.min(waitTime * 2, maxWaitTime);
		    comms.messagesLock.lock();
		    comms = new Comms(serverIP, App.PORT);
		}	
	}
	
	public void close() {
		closed = true;
	}
	
}
