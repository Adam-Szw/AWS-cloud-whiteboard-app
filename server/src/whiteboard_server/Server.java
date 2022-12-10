package whiteboard_server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main server class that creates and manages connections as well as synchronising
 * state updates across clients and other servers.
 * 
 * @author aks60
 *
 */
public class Server {
	
	public static final int UPDATE_TICKRATE = 100;
	public static final int COMMS_TICKRATE = 10;
	public static final boolean DEBUG_MODE = true;
	public static final boolean PRINT_MSG = true;
	
	public static final int SERVER_PORT = 6668;
	
	public int port;
	public ServerSocket serverSocket;
	public Lock stateLock = new ReentrantLock();
	public State updateState;
	public State stateTotal;
	
	public Lock connectionsLock = new ReentrantLock();
	List<Connection> clientConnections = new ArrayList<Connection>();
	List<Connection> serverConnections = new ArrayList<Connection>();
	List<String> connectedServers = new ArrayList<String>();
	
	public Lock IPlock = new ReentrantLock();
	
	public Server(int port) {
		this.port = port;
		this.updateState = new State();
		this.stateTotal = new State();
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error encountered while opening server socket on port: " + port);
			e.printStackTrace();
		}
	}
	
	public static void sleepThread(String err, int tickrate) {
		try {
			Thread.sleep(tickrate);
		} catch (InterruptedException e) {
			System.out.println(err + " thread interrupted");
			e.printStackTrace();
		}
	}
	
	// Sends a state change to all clients connected
	public void updateClientStates(State state) {
		stateLock.lock();
		connectionsLock.lock();
		for(Connection connection : clientConnections) {
			connection.sendState(state);
		}
		state.clear();
		connectionsLock.unlock();
		stateLock.unlock();
	}
	
	public void broadcastServers(String update) {
		connectionsLock.lock();
		for(Connection connection : serverConnections) {
			connection.sendMessage(update);
		}
		connectionsLock.unlock();
	}
	
	public void requestStates() {
		connectionsLock.lock();
		for(Connection connection : serverConnections) {
			connection.requestState();
		}
		connectionsLock.unlock();
	}

	public static void main(String[] args){
		Server server = new Server(SERVER_PORT);
		
		// Communication threads
		ConnectionChecker checker = new ConnectionChecker(server);
		Thread checkThread = new Thread(checker);
		checkThread.start();
		ClientUpdater updater = new ClientUpdater(server);
		Thread updateThread = new Thread(updater);
		updateThread.start();
		
		// Open server connection thread
		sleepThread("Main thread", UPDATE_TICKRATE * 10);
		ServerPeerAccepter peerAccepter = new ServerPeerAccepter(SERVER_PORT, server);
		Thread peerThread = new Thread(peerAccepter);
		peerThread.start();
		// Wait for it to finish
		while(!peerAccepter.initialized) sleepThread("Main thread", UPDATE_TICKRATE);
		
		// Open client connection thread
		ClientAccepter accepter = new ClientAccepter(server, peerAccepter);
		Thread accepterThread = new Thread(accepter);
		accepterThread.start();
		
		// Get up to date with other servers
		// Give some time for other servers to be picked up by peer accepter
		while(!peerAccepter.serverInitCheck) sleepThread("Main thread", UPDATE_TICKRATE);
		server.requestStates();
	}

}
