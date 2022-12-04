package whiteboard_server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main server class that creates and manages connections as well as synchronising
 * state updates accross clients and other servers.
 * 
 * @author aks60
 *
 */
public class Server {
	
	public static final int UPDATE_TICKRATE = 100;
	public static final int COMMS_TICKRATE = 10;
	public static final boolean DEBUG_MODE = true;
	
	public static final int serverPort = 6668;
	
	public int port;
	public ServerSocket serverSocket;
	public Lock stateLock = new ReentrantLock();
	public State updateState;
	public State stateTotal;
	
	List<Connection> clientConnections = new ArrayList<Connection>();
	List<Connection> serverConnections = new ArrayList<Connection>();
	
	// List of available servers to connect to on the network
	// They should be source from AWS elastic IP service
	@SuppressWarnings("serial")
	public static final ArrayList<String> serverIPs = new ArrayList<String>() {
		{
			
		}
	};
	
	public Server(int port) throws IOException {
		this.port = port;
		this.updateState = new State();
		this.stateTotal = new State();
		serverSocket = new ServerSocket(port);
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
		for(Connection connection : clientConnections) {
			connection.sendState(state);
		}
		state.clear();
		stateLock.unlock();
	}
	
	public void broadcastServers(String update) {
		for(Connection connection : serverConnections) {
			connection.sendMessage(update);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = new Server(serverPort);
		
		// Create and start threads for various operations
		ClientAccepter accepter = new ClientAccepter(server);
		Thread accepterThread = new Thread(accepter);
		accepterThread.start();
		ConnectionChecker checker = new ConnectionChecker(server);
		Thread checkThread = new Thread(checker);
		checkThread.start();
		ClientUpdater updater = new ClientUpdater(server);
		Thread updateThread = new Thread(updater);
		updateThread.start();
		ServerPeerAccepter peerAccepter = new ServerPeerAccepter(serverIPs, serverPort, server);
		Thread peerThread = new Thread(peerAccepter);
		peerThread.start();
		
	}

}
