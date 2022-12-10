package whiteboard_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for establishing, sending and receiving messages
 * to and from clients and other servers.
 * 
 * @author aks60
 * 
 */
public class Connection implements Runnable {
	
	private Server server;
	
	private DataInputStream din;
	private DataOutputStream dout;
	private Socket socket;
	
	private Lock messageLock = new ReentrantLock();					// Lock for messages below
	private List<String> messages = new ArrayList<String>();		// Messages to send on next tick
	private List<String> serverUpdates = new ArrayList<String>();	// Container to gather new state from another server
	private boolean serverUpdateGathered = false;
	
	private boolean serverConnection;
	
	public boolean closed = false;
	
	public String ip;
	
	public Connection(Server server, Socket s, boolean serverConnection, String ip) throws IOException {
		this.server = server;
		this.socket = s;
		this.ip = ip;
		din = new DataInputStream(s.getInputStream());
		dout = new DataOutputStream(s.getOutputStream());
		this.serverConnection = serverConnection;
	}
	
	/*
	 * Open threads for receiving and sending messages. The threads will permanently stop
	 * when connection is lost and then a new connection needs to be established.
	 */
	@Override
	public void run() {
		//Thread for sending out messages
		Thread responder = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!closed) {
					if(messages.size() != 0) writeMessage();
					else {
						Server.sleepThread("Responder thread", Server.COMMS_TICKRATE);
					}
				}
			}
		});
		responder.start();
		
		// Thread for receiving messages
		Thread receiver = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!closed) {
					receiveMessage();
				}
			}
		});
		receiver.start();
		
		Server.sleepThread("Connection thread", Server.UPDATE_TICKRATE);
		if(!serverConnection) {
			server.stateLock.lock();
			sendState(server.stateTotal);
			server.stateLock.unlock();
		}
	}
	
	// Sends a message about new state of the system
	public void sendState(State state) {
		messageLock.lock();
		for(int i = 0; i < state.updates.size(); i++) {
			addMessage("UPDATE;" + state.updates.get(i));
		}
		addMessage("COUNT;" + state.stateID + ";");		// Send state update ID
		messageLock.unlock();
	}
	
	// Same as above but prepared to be parsed by server
	public void sendStateServ(State state) {
		messageLock.lock();
		server.stateLock.lock();
		addMessage("COUNT:" + state.stateID + ";");
		for(int i = 0; i < state.updates.size(); i++) {
			addMessage("SRVUP:" + state.updates.get(i));
		}
		addMessage("SRVUPEND;");
		server.stateLock.unlock();
		messageLock.unlock();
	}
	
	// Send message on the connection
	public void sendMessage(String message) {
		messageLock.lock();
		addMessage(message);
		messageLock.unlock();
	}
	
	// Request current full state from other servers to synchronise
	public void requestState() {
		if(Server.DEBUG_MODE) System.out.println("Requesting full state update from other servers");
		messageLock.lock();
		addMessage("SERV_FETCH;");
		messageLock.unlock();
	}
	
	// This function takes care of string overflow and separates messages into chunks
	private void addMessage(String message) {
		if(messages.size() == 0) messages.add("");
		String currMsg = messages.get(messages.size() - 1);
		if(currMsg.length() + message.length() > 10000) {
			messages.add(message + "\n");
		} else {
			messages.set(messages.size() - 1, currMsg + message + "\n");
		}
	}
	
	private void writeMessage(){
		try {
			messageLock.lock();
			while(messages.size() > 0) {
				dout.writeUTF(messages.get(0));
				if(Server.PRINT_MSG) System.out.println("OUT: " + (messages.get(0)));
				dout.flush();
				messages.remove(0);
			}
			messageLock.unlock();
		} catch (Exception e) {
			// Connection to client lost
			close();
		}
	}
	
	private void receiveMessage() {
		try {
			String str = din.readUTF();
			if(Server.PRINT_MSG) System.out.println("IN: " + str);
			server.stateLock.lock();
			decodeMessage(str);
			server.stateLock.unlock();
		} catch (Exception e) {
			// Connection to client lost
			close();
		}
	}
	
	private void decodeMessage(String message) {
		for(String str : message.replace("\n", "@").split("@")) {
			if(strBegins(str, "COUNT:")) {
				// Another server sent total state update
				// parse state and decide if update or not based on id
				String msg = str.replaceAll("\\s","");
				int idend = msg.indexOf(";", 6);
				String sub = msg.substring(6, idend);
				long idReceived = Long.parseLong(sub);
				if(idReceived > server.stateTotal.stateID) {
					// Be generous with timeout here - it might be a lot of data
					updateServerTotalState(idReceived, Server.UPDATE_TICKRATE * 100);
				}
			} else if(strBegins(str, "CLEAR;")) {
				// Clear total state signal sent
				if(Server.DEBUG_MODE) System.out.println("Server state clear signal received");
				String id = str.substring(6);
				server.stateLock.lock();
				server.updateState.clearUpdate();
				server.stateTotal.clearUpdate();
				server.stateLock.unlock();
				sendMessage("ACK;" + id);
				// Update clients after this
				ClientUpdater.periodicCheck = true;
				// Forward to other servers
				if(!serverConnection) server.broadcastServers(str);
			} else if(strBegins(str, "SERV_FETCH;")) {
				// Another server requests total state
				if(Server.DEBUG_MODE) System.out.println("Server full state request received");
				sendStateServ(server.stateTotal);
			} else if(strBegins(str, "SRVUP:")) {
				// Part of the server update package
				serverUpdates.add(str.substring(6));
			} else if(strBegins(str, "SRVUPEND;")) {
				// End of server update package
				serverUpdateGathered = true;
			} else if(strBegins(str, "FETCH_HISTORY;")) {
				// The connection is requesting full state
				if(Server.DEBUG_MODE) System.out.println("Client full state request received");
				server.stateLock.lock();
				sendState(server.stateTotal);
				server.stateLock.unlock();
				String id = str.substring(14);
				sendMessage("ACK;" + id);
			} else {
				String update = "";
				for(String msg : str.replace(";", ";@").split("@")) {
					// The connection is just updating the state
					msg = msg.replaceAll("\\s","");
					update += msg;
				}
				if(update.length() > 0) {
					// Acknowledge state update from client
					server.updateState.updateState(update);
					server.stateTotal.updateState(update);
					// Prevent loop between servers
					if(!serverConnection) server.broadcastServers(update);
					if(!serverConnection) sendMessage("ACK;" + update);
				}
			}
		}
	}
	
	private void updateServerTotalState(long id, long timeout) {
		// Gather complete state package first then use it to update
		Thread gatherer = new Thread(new Runnable() {
			@Override
			public void run() {
				long start = System.currentTimeMillis();
				if(Server.DEBUG_MODE) System.out.println("Listening for sever state updates");
				while(!serverUpdateGathered) {
					long passed = System.currentTimeMillis() - start;
					if(passed > timeout) {
						if(Server.DEBUG_MODE) System.out.println("Failed to gather server state");
						return;
					}
					Server.sleepThread("Gatherer thread", Server.UPDATE_TICKRATE);
				}
				if(Server.DEBUG_MODE) System.out.println("New server state acquired");
				server.stateLock.lock();
				server.stateTotal.clear();
				for(String update : serverUpdates) {
					server.stateTotal.updateState(update);
					server.updateState.updateState(update);
				}
				serverUpdates.clear();
				server.stateTotal.stateID = id;
				server.updateState.stateID = id;
				server.stateLock.unlock();
			}
		});
		gatherer.start();
	}
	
	public static boolean strBegins(String str, String compare) {
		int length = compare.length();
		if(length > str.length()) return false;
		String sub = str.substring(0, length);
		if(sub.equals(compare)) return true;
		return false;
	}
	
	// Stops connected threads and close socket
	private void close() {
		if(closed) return;
		if(Server.DEBUG_MODE) System.out.println("Closing connection with: " + ip);
		try {
			din.close();
			dout.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Error encountered while closing connection");
			e.printStackTrace();
		}
		closed = true;
	}
}
