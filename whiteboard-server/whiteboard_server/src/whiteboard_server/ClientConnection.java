package whiteboard_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
	String message = "";
	
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
							if(!message.equals("")) writeMessage();
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
		this.message += message + "\n";
		messageLock.unlock();
	}
	
	void writeMessage() throws IOException {
		messageLock.lock();
		System.out.print("sent message " + message);
		dout.writeUTF(message);
		dout.flush();
		message = "";
		messageLock.unlock();
	}
	
	void receiveMessage() throws IOException {
		String str = din.readUTF();
		System.out.println("received message " + str);
		server.clientsUpdatedLock.lock();
		server.state.updateState(str);
		server.clientsUpdated = false;
		addMessage("ACK;" + str);
		server.clientsUpdatedLock.unlock();
	}
}
