package whiteboard_server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
	
	static final int SERVER_PORT = 6668;
	static final int UPDATE_TICKRATE = 100;
	
	public int port;
	public ServerSocket serverSocket;
	public Lock stateLock = new ReentrantLock();
	public State updateState;
	public State stateTotal;
	
	List<ClientConnection> connections = new ArrayList<ClientConnection>();
	
	public Server(int port) throws IOException {
		this.port = port;
		this.updateState = new State();
		this.stateTotal = new State();
		serverSocket = new ServerSocket(port);
	}
	
	public void updateClientStates(State state) throws IOException {
		stateLock.lock();
		for(ClientConnection connection : connections) {
			connection.sendState(state);
		}
		state.clear();
		stateLock.unlock();
	}
	
	public void updateOtherServers() {
		//todo
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = new Server(SERVER_PORT);
		ClientAccepter accepter = new ClientAccepter(server);
		Thread accepterThread = new Thread(accepter);
		accepterThread.start();
		while(true) {
			server.updateClientStates(server.updateState);
			Thread.sleep(UPDATE_TICKRATE);
		}
	}

}
