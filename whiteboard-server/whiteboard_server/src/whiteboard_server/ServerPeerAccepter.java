package whiteboard_server;

import java.net.ConnectException;
import java.net.Socket;
import java.util.List;

/**
 * Creates connections to other servers.
 * 
 * @author aks60
 *
 */
public class ServerPeerAccepter implements Runnable {
	
	private List<String> ips;
	private int port;
	private Socket socket;
	private Server server;
	
	public ServerPeerAccepter(List<String> ips, int port, Server server) {
		this.ips = ips;
		this.port = port;
		this.server = server;
	}

	@Override
	public void run() {
		for(int i = 0; i < ips.size(); i++) {
			String host = ips.get(i);
			try {
				socket = new Socket(host, port);
				if(Server.DEBUG_MODE) System.out.println("New server peer connection established");
				Connection connection = new Connection(server, socket);
				Thread connThread = new Thread(connection);
				connThread.start();
				server.serverConnections.add(connection);
			} catch(ConnectException e) {
				// Failed to connect to the server. This is expected - move on to the next one
				continue;
			} catch(Exception e){
				System.out.println("Error encountered while connecting to another server");
				e.printStackTrace();
			}
		}
		// New server connections will not be made often
		Server.sleepThread("Peer accepter thread", Server.UPDATE_TICKRATE);
	}

}