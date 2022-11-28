package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Comms implements Runnable {
	
	String host;
	int port;
	
	DataInputStream din;
	DataOutputStream dout;
	
	public String message = "";
	
	public Comms(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	void postMessage() throws IOException {
		dout.writeUTF(message);
		dout.flush();
		message = "";
	}
	
	void receiveMessage() throws IOException {
		String response = din.readUTF();
		System.out.println("Server sent response: " + response);
		if(!response.equals("ack")) {
			App.currentState = response;
			App.stateChanged = true;
		}
	}

	@Override
	public void run() {
		
		try {
			Socket s = new Socket(host, port);
			din = new DataInputStream(s.getInputStream());
			dout = new DataOutputStream(s.getOutputStream());
			
			Thread senderThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							if(message.equals("")) {
								Thread.sleep(10);
							} else {
								postMessage();
							}
						} catch(Exception e){ System.out.println(e); }
					}
					
				}
			});
			senderThread.start();
			
			Thread receiverThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true) {
						try {
							receiveMessage();
							Thread.sleep(10);
						} catch(Exception e){ System.out.println(e); }
					}
				}
			});
			receiverThread.start();
			
		} catch(Exception e){ System.out.println(e); }
	}
	
}