package whiteboard_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	
	public ServerPeerAccepter(int port, Server server) {
		this.port = port;
		this.server = server;
		addExistingServerIPs();
		removeMyIpFromList();
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
			 JSONObject json = new JSONObject(jsonTotal);
			 int reservationCount = json.getJSONArray("Reservations").length();
			 for(int i = 0; i < reservationCount; i++) {
				 JSONArray instances = json.getJSONArray("Reservations").getJSONObject(i).getJSONArray("Instances");
				 for (int j = 0; j < instances.length(); j++) {
			          JSONObject instance = instances.getJSONObject(j);
			          String publicIp = instance.getString("PublicIpAddress");
			          serverIPs.add(publicIp);
				 }
			 }
		} catch (IOException e) {
			System.out.println("Failed to launch aws ec2 describe-instances command");
			e.printStackTrace();
		} catch (JSONException e) {
			System.out.println("Failed to parse received JSON from ec2 instances command");
			e.printStackTrace();
		}
	}
	
	private void removeMyIpFromList() {
		String urlString = "http://checkip.amazonaws.com/";
		URL url;
		try {
			url = new URL(urlString);
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String IP = br.readLine();
			serverIPs.remove(IP);
		} catch (Exception e) {
			System.out.println("Couldnt connect to amazon website to check IP");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(true) {
			for(int i = 0; i < serverIPs.size(); i++) {
				String host = serverIPs.get(i);
				try {
					//todo - timeout this!!
					socket = new Socket(host, port);
					server.connectionsLock.lock();
					if(server.connectedServers.contains(host)) {
						server.connectionsLock.unlock();
						continue;
					}
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
			// New server connections will not be made often
			Server.sleepThread("Peer accepter thread", Server.UPDATE_TICKRATE);
		}
	}

}
