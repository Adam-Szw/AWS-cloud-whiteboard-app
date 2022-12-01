package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Communication with the server
 * 
 * @author aks60
 *
 */
public class Comms implements Runnable {
	
	String host;
	int port;
	
	DataInputStream din;
	DataOutputStream dout;
	
	private String messageToSend = "";
	private Lock messageSendLock = new ReentrantLock();
	
	public Lock messagesLock = new ReentrantLock();
	public List<Long> confirmations;
	public List<UpdateGroup> stateUpdates;
	public long serverStateID = 0;
	
	public Comms(String host, int port) {
		this.host = host;
		this.port = port;
		this.confirmations = new ArrayList<Long>();
		this.stateUpdates = new ArrayList<UpdateGroup>();
	}
	
	void addMessage(String str) {
		messageSendLock.lock();
		messageToSend += str;
		messageSendLock.unlock();
	}
	
	void postMessage() throws IOException {
		messageSendLock.lock();
		dout.writeUTF(messageToSend);
		dout.flush();
		messageToSend = "";
		messageSendLock.unlock();
	}
	
	void receiveMessage() throws IOException {
		String response = din.readUTF();
		messagesLock.lock();
		String[] responses = response.split("\n");
		for(String msg : responses) {
			sortMessage(msg);
		}
		messagesLock.unlock();
	}
	
	void sortMessage(String msg) {
		if(msg.length() >= 4 && msg.substring(0, 4).equals("ACK;")) {
			int idloc = msg.indexOf("ID:") + 3;
			int idend = msg.indexOf(";", idloc);
			long id = Long.parseLong(msg.substring(idloc, idend));
			confirmations.add(id);
		}
		
		if(msg.length() >= 7 && msg.substring(0, 7).equals("UPDATE;")) {
			int idloc = msg.indexOf("ID:") + 3;
			int idend = msg.indexOf(";", idloc);
			String id = msg.substring(idloc, idend);
			UpdateGroup newUpdate = new UpdateGroup(Long.parseLong(id));
			for(String str : msg.substring(idend).split(";")) {
				newUpdate.append(str);
			}
			stateUpdates.add(newUpdate);
		}
		
		if(msg.length() >= 6 && msg.substring(0, 6).equals("COUNT;")) {
			int idend = msg.indexOf(";", 6);
			String str = msg.substring(6, idend);
			serverStateID = Math.max(serverStateID, Long.parseLong(str));
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
							if(messageToSend.equals("")) {
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
