package whiteboard_server;

import java.io.IOException;
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
	
	final int UPDATE_TICKRATE = 100;
	public static final boolean DEBUG_MODE = true;
	
	public int port;
	public ServerSocket serverSocket;
	public Lock stateLock = new ReentrantLock();
	public State updateState;
	public State stateTotal;
	
	List<ClientConnection> clientConnections = new ArrayList<ClientConnection>();
	
	public Server(int port) throws IOException {
		this.port = port;
		this.updateState = new State();
		this.stateTotal = new State();
		serverSocket = new ServerSocket(port);
	}
	
	// Sends a state change to all clients connected
	public void updateClientStates(State state) throws IOException {
		stateLock.lock();
		for(ClientConnection connection : clientConnections) {
			connection.sendState(state);
		}
		state.clear();
		stateLock.unlock();
	}
	
	public void updateOtherServers() {
		//todo
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = new Server(6668);
		
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

	}

}
