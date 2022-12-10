package whiteboard_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

/**
 * Creates connections to other servers.
 * 
 * @author aks60
 *
 */
public class ServerPeerAccepter implements Runnable {
	
	public ArrayList<String> serverIPs = new ArrayList<String>();

	private int port;
	private Socket socket;
	private Server server;
	
	private String myIP;
	
	public boolean initialized = false;
	public boolean serverInitCheck = false;
	
	public ServerPeerAccepter(int port, Server server) {
		this.port = port;
		this.server = server;
		myIP = getMyIpAddress();
		addExistingServerIPs();
		initialized = true;
	}
	
	private void addExistingServerIPs() {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("aws", "ec2", "describe-instances");
			Process process = processBuilder.start();
			InputStreamReader isr = new InputStreamReader(process.getInputStream());
			BufferedReader reader = new BufferedReader(isr);
			String jsonTotal = "";
			String line;
			while ((line = reader.readLine()) != null) {
				jsonTotal += line + "\n";
			}
			for(String str : jsonTotal.split("\n")) {
				if(str.contains("PublicIpAddress")) {
					str = str.replaceAll("\\s","");
					String ip = str.substring(19, str.length()-2);
					if(!serverIPs.contains(ip) && !ip.equals(myIP)) {
						serverIPs.add(ip);
						if(Server.DEBUG_MODE) System.out.println("Detected server under IP: " + ip);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Failed to launch aws ec2 describe-instances command");
			e.printStackTrace();
		}
	}
	
	private String getMyIpAddress() {
		String urlString = "http://checkip.amazonaws.com/";
		URL url;
		String IP = "";
		try {
			url = new URL(urlString);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			IP = br.readLine();
		} catch (Exception e) {
			System.out.println("Couldnt connect to amazon website to check IP");
			e.printStackTrace();
		}
		return IP;
	}

	@Override
	public void run() {
		while(true) {
			server.IPlock.lock();
			addExistingServerIPs();
			@SuppressWarnings("unchecked")
			ArrayList<String> serverIPsCopy = (ArrayList<String>) serverIPs.clone();
			server.IPlock.unlock();
			for(int i = 0; i < serverIPsCopy.size(); i++) {
				String host = serverIPsCopy.get(i);
				try {
					server.connectionsLock.lock();
					if(server.connectedServers.contains(host)) {
						server.connectionsLock.unlock();
						continue;
					}
					server.connectionsLock.unlock();
					socket = new Socket(host, port);
					server.connectionsLock.lock();
					if(Server.DEBUG_MODE) System.out.println("New server peer connection established with: " + host);
					Connection connection = new Connection(server, socket, true, host);
					Thread connThread = new Thread(connection);
					connThread.start();
					server.serverConnections.add(connection);
					server.connectedServers.add(host);
					server.connectionsLock.unlock();
				} catch(ConnectException e){
					// This is expected. move on to the next connection
					continue;
				} catch(Exception e){
					System.out.println("Error encountered while connecting to another server");
					e.printStackTrace();
				}
			}
			serverInitCheck = true;
			// New server connections will not be made often
			Server.sleepThread("Peer accepter thread", Server.UPDATE_TICKRATE);
		}
	}

}
