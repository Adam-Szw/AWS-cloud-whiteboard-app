package whiteboard_server;

/**
 * Checks if connections are still active and closes them if they are not.
 * This saves resources as the server will not be needlessly sending messages
 * to closed connections.
 * 
 * @author aks60
 *
 */
public class ConnectionChecker implements Runnable {

	private Server server;
	
	public ConnectionChecker(Server server) {
		this.server = server;
	}
	
	@Override
	public void run() {
		for(Connection connection : server.clientConnections) {
			if(connection.closed) {
				if(Server.DEBUG_MODE) System.out.println("Removing client comms from the list");
				server.clientConnections.remove(connection);
				break;
			}
		}
		Server.sleepThread("Connection checker thread", Server.UPDATE_TICKRATE);
	}

}
