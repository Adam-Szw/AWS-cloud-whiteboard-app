package whiteboard_server;

import java.net.Socket;
import java.util.ArrayList;

/**
 * Creates client connections.
 * 
 * @author aks60
 * 
 */
public class ClientAccepter implements Runnable {

	private Server server;
	private ServerPeerAccepter peerAccepter;
	
	public ClientAccepter(Server server, ServerPeerAccepter peerAccepter) {
		this.server = server;
		this.peerAccepter = peerAccepter;
	}
	
	@Override
	public void run() {
		while(true) {
			/*
			 * Await client to connect and if received - create new connection execution
			 */
			try {
				server.IPlock.lock();
				@SuppressWarnings("unchecked")
				ArrayList<String> serverIPsCopy = (ArrayList<String>) peerAccepter.serverIPs.clone();
				server.IPlock.unlock();
				Socket connectionSocket = server.serverSocket.accept();
				String receivedIP = connectionSocket.getRemoteSocketAddress().toString();
				receivedIP = receivedIP.substring(1, receivedIP.indexOf(":"));
				server.connectionsLock.lock();
				if(server.connectedServers.contains(receivedIP)) {
					server.connectionsLock.unlock();
					continue;
				}
				if(serverIPsCopy.contains(receivedIP)) {
					if(Server.DEBUG_MODE) System.out.println("New server peer connection accetped from: " + receivedIP);
					Connection connection = new Connection(server, connectionSocket, true, receivedIP);
					Thread connThread = new Thread(connection);
					connThread.start();
					server.connectedServers.add(receivedIP);
					server.serverConnections.add(connection);
				} else {
					if(Server.DEBUG_MODE) System.out.println("New client connection established with: " + receivedIP);
					Connection connection = new Connection(server, connectionSocket, false, receivedIP);
					Thread connThread = new Thread(connection);
					connThread.start();
					server.clientConnections.add(connection);
				}
				server.connectionsLock.unlock();
			} catch(Exception e){
				System.out.println("Connection error");
				e.printStackTrace();
			} finally {
				Server.sleepThread("Connector thread", Server.UPDATE_TICKRATE);
			}
		}
	}

}
