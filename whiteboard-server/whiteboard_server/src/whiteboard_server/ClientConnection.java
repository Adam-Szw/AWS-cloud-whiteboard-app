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
		server.state.updateState(str);
		System.out.println("server size: " + server.state.updates.size());
		addMessage("ACK;" + str);
	}
}
