package whiteboard_server;


/**
 * Periodically sends servers' updates to clients
 * 
 * @author aks60
 *
 */
public class ClientUpdater implements Runnable {
	
	private Server server;
	
	public static boolean periodicCheck = false;
	
	public ClientUpdater(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		long passed = 0;
		
		while(true) {
			if(server.updateState.updates.size() > 0 || periodicCheck) {
				server.updateClientStates(server.updateState);
				periodicCheck = false;
			}
			passed = System.currentTimeMillis() - start;
			if(passed > Server.UPDATE_TICKRATE * 50) {
				start = System.currentTimeMillis();
				periodicCheck = true;
			}
			Server.sleepThread("Updater thread", Server.UPDATE_TICKRATE);
		}
	}

}
