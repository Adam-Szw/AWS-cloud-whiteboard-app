package whiteboard_server;

import java.net.Socket;

/**
 * Creates client connections.
 * 
 * @author aks60
 * 
 */
public class ClientAccepter implements Runnable {

	private Server server;
	
	public ClientAccepter(Server server) {
		this.server = server;
	}
	
	@Override
	public void run() {
		while(true) {
			/*
			 * Await client to connect and if received - create new connection execution
			 */
			try {
				Socket connectionSocket = server.serverSocket.accept();
				if(Server.DEBUG_MODE) System.out.println("New client connection established");
				Connection connection = new Connection(server, connectionSocket);
				Thread connThread = new Thread(connection);
				connThread.start();
				server.clientConnections.add(connection);
			} catch(Exception e){
				System.out.println("Connection error");
				e.printStackTrace();
			}
		}
	}

}
