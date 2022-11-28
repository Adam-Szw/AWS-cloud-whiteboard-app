package whiteboard_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientConnection implements Runnable {
	
	Server server;
	int port;
	ServerSocket ss;
	Socket s;
	
	DataInputStream din;
	DataOutputStream dout;
	
	String message = "";
	boolean stateUploaded = false;
	
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
	
	void addStateMessage(String message) {
		if(!stateUploaded) {
			this.message += message + "\n";
			stateUploaded = true;
		}
	}
	
	void writeMessage() throws IOException {
		dout.writeUTF(message);
		dout.flush();
		System.out.println("sent message " + message);
		message = "";
		stateUploaded = false;
	}
	
	void receiveMessage() throws IOException {
		String str = din.readUTF();
		System.out.println("received message " + str);
		server.serverState += str;
		server.clientsUpdatedLock.lock();
		server.clientsUpdated = false;
		server.clientsUpdatedLock.unlock();
	}
}
