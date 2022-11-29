package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Synchroniser {
	
	public Lock updateLock = new ReentrantLock();
	public UpdateGroup currentUpdate;
	
	public Lock stateLock = new ReentrantLock();
	private List<UpdateGroup> state;
	
	private Comms comms;
	private GraphicsImplementer implementer;
	
	public static int CLIENT_TICKRATE = 100;
	
	public Synchroniser(Comms comms, GraphicsImplementer implementer) {
		this.comms = comms;
		this.implementer = implementer;
		currentUpdate = new UpdateGroup();
		state = new ArrayList<UpdateGroup>();
	}
	
	public void start() {
		Thread stateUploader = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					if(currentUpdate.empty) {
						try {
							Thread.sleep(CLIENT_TICKRATE);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						continue;
					}
					
					//send current state change
					updateLock.lock();
					comms.messageToSend = currentUpdate.toString();
					stateLock.lock();
					state.add(currentUpdate);
					stateLock.unlock();
					long groupID = currentUpdate.id;
					currentUpdate = new UpdateGroup();
					updateLock.unlock();
					
					//wait for confirmation
					boolean awaitingAck = true;
					while(awaitingAck) {
						comms.messagesLock.lock();
						int i = comms.confirmations.indexOf(groupID);
						if(i != -1) {
							awaitingAck = false;
							comms.confirmations.remove(i);
						}
						comms.messagesLock.unlock();
					}
					//todo - timeout if confirmation NOT received
					
					try {
						Thread.sleep(CLIENT_TICKRATE);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		stateUploader.start();
		
		Thread stateReceiver = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					//receive state from server
					comms.messagesLock.lock();
					int i = comms.stateUpdates.size();
					if(i > 0) {
						stateLock.lock();
						compareStates(state, comms.stateUpdates);
						stateLock.unlock();
						comms.stateUpdates.clear();
						comms.messagesLock.unlock();
					}
					else {
						comms.messagesLock.unlock();
						try {
							Thread.sleep(CLIENT_TICKRATE);
							continue;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		stateReceiver.start();
	}
	
	public void compareStates(List<UpdateGroup> state, List<UpdateGroup> update) {
		Collections.sort(state);
		Collections.sort(update);
		//check for missing update IDs and implement gaps
		for(int i = 0; i < update.size(); i++) {
			boolean found = false;
			for(int j = 0; j < state.size(); j++) {
				if(state.get(j).id == update.get(i).id) {
					found = true;
					break;
				}
			}
			if(!found) {
				state.add(update.get(i));
				implementer.implement(update.get(i));
			}
		}
		
	}

}
