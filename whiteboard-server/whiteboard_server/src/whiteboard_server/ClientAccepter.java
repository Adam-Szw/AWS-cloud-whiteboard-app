package whiteboard_server;

import java.net.Socket;

public class ClientAccepter implements Runnable {

	Server server;
	int port;
	
	boolean close = false;
	
	public ClientAccepter(Server server) {
		this.server = server;
		this.port = server.port;
	}
	
	@Override
	public void run() {
		while(!close) {
			try {
				Socket clientSocket = server.serverSocket.accept();
				ClientConnection connection = new ClientConnection(server, port, server.serverSocket, clientSocket);
				Thread connThread = new Thread(connection);
				connThread.start();
				server.connections.add(connection);
			} catch(Exception e){System.out.println(e);}
		}
	}
	
	public void close() {
		close = true;
	}

}
