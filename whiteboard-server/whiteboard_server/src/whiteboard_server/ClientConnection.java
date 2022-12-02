package whiteboard_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is responsible for establishing, sending and receiving messages
 * to and from clients.
 * 
 * @author aks60
 * 
 */
public class ClientConnection implements Runnable {
	
	private Server server;
	
	private DataInputStream din;
	private DataOutputStream dout;
	private Socket socket;
	
	private Lock messageLock = new ReentrantLock();				// Lock for messages below
	private List<String> messages = new ArrayList<String>();	// Messages to send on next tick to connected client
	
	public boolean closed = false;
	
	public ClientConnection(Server server, Socket s) throws IOException {
		this.server = server;
		this.socket = s;
		din = new DataInputStream(s.getInputStream());
		dout = new DataOutputStream(s.getOutputStream());
	}
	
	/*
	 * Open threads for receiving and sending messages. The threads will permanently stop
	 * when connection is lost with the client and then a new connection needs to be established.
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
	}
	
	// Sends a message about new state of the system
	public void sendState(State state) {
		for(int i = 0; i < state.updates.size(); i++) {
			addMessage("UPDATE;" + state.updates.get(i));
		}
		addMessage("COUNT;" + state.stateID + ";");		// Send state update ID
	}
	
	void addMessage(String message) {
		// Implementation below is to avoid String being overflowed
		messageLock.lock();
		if(messages.size() == 0) messages.add("");
		String currMsg = messages.get(messages.size() - 1);
		if(currMsg.length() + message.length() > 10000) {
			messages.add(message + "\n");
		} else {
			messages.set(messages.size() - 1, currMsg + message + "\n");
		}
		messageLock.unlock();
	}
	
	void writeMessage(){
		try {
			messageLock.lock();
			while(messages.size() > 0) {
				dout.writeUTF(messages.get(0));
				dout.flush();
				messages.remove(0);
			}
			messageLock.unlock();
		} catch (SocketException e) {
			// Connection to client lost
			close();
		}
		catch (IOException e) {
			System.out.println("Error encountered while writing to connection");
			e.printStackTrace();
		}
	}
	
	void receiveMessage() {
		try {
			String str = din.readUTF();
			server.stateLock.lock();
			String update = "";
			for(String msg : str.replace(";", ";@").split("@")) {
				if(msg.length() >= 14 && msg.substring(0, 14).equals("FETCH_HISTORY;")) {
					// The client is requesting full state
					if(Server.DEBUG_MODE) System.out.println("Client full state request received");
					sendState(server.stateTotal);
				} else {
					// The client is just updating the state
					update += msg;
				}
			}
			if(!update.equals("")) {
				// Acknowledge state update from client
				server.updateState.updateState(update);
				server.stateTotal.updateState(update);
				addMessage("ACK;" + update);
			}
			server.stateLock.unlock();
		} catch (SocketException e) {
			// Connection to client lost
			close();
		} catch (IOException e) {
			System.out.println("Error encountered while receiving message from connection");
			e.printStackTrace();
		}
	}
	
	// Stops connected threads and close socket
	void close() {
		if(Server.DEBUG_MODE) System.out.println("Closing connection with client");
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
