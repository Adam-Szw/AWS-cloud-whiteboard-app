package whiteboard_server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
	
	static final int SERVER_PORT = 6668;
	
	String serverState = "";
	public int port;
	public ServerSocket serverSocket;
	
	List<ClientConnection> connections = new ArrayList<ClientConnection>();
	Lock clientsUpdatedLock = new ReentrantLock();
	Boolean clientsUpdated = true;
	
	public Server(int port) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(port);
	}
	
	public void updateClientStates() throws IOException {
		System.out.println("updating clients");
		for(ClientConnection connection : connections) {
			connection.addStateMessage(serverState);
		}
		clientsUpdated = true;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = new Server(SERVER_PORT);
		ClientAccepter accepter = new ClientAccepter(server);
		Thread accepterThread = new Thread(accepter);
		accepterThread.start();
		while(true) {
			if(!server.clientsUpdated) {
				server.clientsUpdatedLock.lock();
				server.updateClientStates();
				server.clientsUpdatedLock.unlock();
			} else {
				Thread.sleep(10);
			}
		}
	}

}