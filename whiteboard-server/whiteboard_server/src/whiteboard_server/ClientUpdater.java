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
				Thread.sleep(server.UPDATE_TICKRATE);
			} catch (IOException e) {
				System.out.println("Error encountered while updating clients");
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("Updater thread interrupted");
				e.printStackTrace();
			}
		}
	}

}
