package whiteboard_server;

import java.io.IOException;

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
			try {
				server.updateClientStates(server.updateState);
				Server.sleepThread("Updater thread", Server.UPDATE_TICKRATE);
			} catch (IOException e) {
				System.out.println("Error encountered while updating clients");
				e.printStackTrace();
			}
		}
	}

}
