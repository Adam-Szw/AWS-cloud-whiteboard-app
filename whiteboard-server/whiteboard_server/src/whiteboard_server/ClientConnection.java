package whiteboard_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientConnection implements Runnable {
	
	Server server;
	int port;
	ServerSocket ss;
	Socket s;
	
	DataInputStream din;
	DataOutputStream dout;
	
	Lock messageLock = new ReentrantLock();
	List<String> messages = new ArrayList<String>();
	
	public ClientConnection(Server server, int port, ServerSocket ss, Socket s) throws IOException {
		this.server = server;
		this.port = port;
		this.ss = ss;
		this.s = s;
		din = new DataInputStream(s.getInputStream());
		dout = new DataOutputStream(s.getOutputStream());
	}
	
	@Override
	public void run() {
		try {
			Thread responder = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							if(messages.size() != 0) writeMessage();
							else Thread.sleep(10);
						} catch(Exception e){System.out.println(e);}
					}
				}
			});
			responder.start();
			
			Thread receiver = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							receiveMessage();
						} catch(Exception e){System.out.println(e);}
					}
				}
			});
			receiver.start();
			
		} catch(Exception e){System.out.println(e);}
	}
	
	public void sendState(State state) {
		for(int i = 0; i < state.updates.size(); i++) {
			addMessage("UPDATE;" + state.updates.get(i));
		}
		addMessage("COUNT;" + state.stateID + ";");
	}
	
	void addMessage(String message) {
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
	
	void writeMessage() throws IOException {
		messageLock.lock();
		while(messages.size() > 0) {
			dout.writeUTF(messages.get(0));
			dout.flush();
			messages.remove(0);
		}
		messageLock.unlock();
	}
	
	void receiveMessage() throws IOException {
		String str = din.readUTF();
		server.stateLock.lock();
		String update = "";
		for(String msg : str.replace(";", ";@").split("@")) {
			System.out.println("msg: " + msg);
			if(msg.length() >= 14 && msg.substring(0, 14).equals("FETCH_HISTORY;")) {
				System.out.println("fetching total state");
				sendState(server.stateTotal);
			} else {
				update += msg;
			}
		}
		if(!update.equals("")) {
			System.out.println("updating: " + update);
			server.updateState.updateState(update);
			server.stateTotal.updateState(update);
			addMessage("ACK;" + update);
		}
		server.stateLock.unlock();
	}
}
