package whiteboard_server;


/**
 * Periodically sends servers' updates to clients
 * 
 * @author aks60
 *
 */
public class ClientUpdater implements Runnable {
	
	private Server server;
	
	public ClientUpdater(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		while(true) {
			server.updateClientStates(server.updateState);
			Server.sleepThread("Updater thread", Server.UPDATE_TICKRATE);
		}
	}

}