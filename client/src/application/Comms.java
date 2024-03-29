package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
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
	
	public static final String CONN_LOST = "Lost connection to the server";
	
	private String host;
	private int port;
	private Socket socket;
	
	private DataInputStream din;
	private DataOutputStream dout;
	
	private String messageToSend = "";
	private Lock messageSendLock = new ReentrantLock();
	
	public Lock messagesLock = new ReentrantLock();
	public List<Long> confirmations;
	public List<UpdateGroup> stateUpdates;
	public long serverStateID = 0;
	
	public boolean init = false;
	public boolean closed = true;
	
	public Comms(String host, int port) {
		this.host = host;
		this.port = port;
		this.confirmations = new ArrayList<Long>();
		this.stateUpdates = new ArrayList<UpdateGroup>();
	}
	
	@Override
	public void run() {
		try {
			socket = new Socket(host, port);
			din = new DataInputStream(socket.getInputStream());
			dout = new DataOutputStream(socket.getOutputStream());
			closed = false;
			
			Thread senderThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(!closed) {
						postMessage();
						App.sleepThread("Message receiver", App.COMMS_TICKRATE);
					}
					
				}
			});
			senderThread.start();
			
			Thread receiverThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(!closed) {
						receiveMessage();
						App.sleepThread("Message receiver", App.COMMS_TICKRATE);
					}
				}
			});
			receiverThread.start();
			
			init = true;
			
		} catch(ConnectException e) {
			closed = true;
			init = true;
		} catch(Exception e){
			System.out.println("Error encountered while connecting to the server");
			e.printStackTrace();
		}
	}
	
	void addMessage(String str) {
		messageSendLock.lock();
		messageToSend += str;
		messageSendLock.unlock();
	}
	
	void postMessage() {
		try {
			messageSendLock.lock();
			if(messageToSend.equals("")) {
				messageSendLock.unlock();
				return;
			}
			dout.writeUTF(messageToSend);
			dout.flush();
			messageToSend = "";
			messageSendLock.unlock();
		} catch (Exception e) {
			if(App.DEBUG_MODE) System.out.println(CONN_LOST);
			close();
		}
	}
	
	void receiveMessage() {
		try {
			String response = din.readUTF();
			messagesLock.lock();
			String[] responses = response.split("\n");
			for(String msg : responses) {
				sortMessage(msg);
			}
			messagesLock.unlock();
		} catch (Exception e) {
			if(App.DEBUG_MODE) System.out.println(CONN_LOST);
			close();
		}
	}
	
	void sortMessage(String msg) {
		try {
			if(GraphicsImplementer.strBegins(msg, "ACK;")) {
				int idloc = msg.indexOf("ID:") + 3;
				int idend = msg.indexOf(";", idloc);
				long id = Long.parseLong(msg.substring(idloc, idend));
				confirmations.add(id);
			}
			
			if(GraphicsImplementer.strBegins(msg, "UPDATE;")) {
				int idloc = msg.indexOf("ID:") + 3;
				int idend = msg.indexOf(";", idloc);
				String id = msg.substring(idloc, idend);
				UpdateGroup newUpdate = new UpdateGroup(Long.parseLong(id));
				for(String str : msg.substring(idend).split(";")) {
					newUpdate.append(str);
				}
				stateUpdates.add(newUpdate);
			}
			
			if(GraphicsImplementer.strBegins(msg, "COUNT;")) {
				int idend = msg.indexOf(";", 6);
				String str = msg.substring(6, idend);
				serverStateID = Long.parseLong(str);
			}
		} catch(NumberFormatException e) {
			System.out.println("Unable to sort message: " + msg);
		}
	}
	
	// Closes threads and sockets
	void close() {
		try {
			if(App.DEBUG_MODE) System.out.println("Closing server connection socket");
			din.close();
			dout.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Error encountered while closing socket");
			e.printStackTrace();
		}
		closed = true;
	}
	
}
