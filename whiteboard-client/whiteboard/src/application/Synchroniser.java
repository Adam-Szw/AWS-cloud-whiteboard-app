package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Synchroniser {
	
	public UpdateGroup currentUpdate;
	private List<UpdateGroup> state;
	
	private Comms comms;
	private GraphicsImplementer implementer;
	
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
							Thread.sleep(10);
							continue;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					//send current state change
					comms.messageToSend = currentUpdate.toString();
					state.add(currentUpdate);
					long groupID = currentUpdate.id;
					currentUpdate = new UpdateGroup();
					System.out.println("awaiting confirmation for:" + groupID);
					
					//wait for confirmation
					boolean awaitingAck = true;
					while(awaitingAck) {
						comms.messagesLock.lock();
						int i = comms.confirmations.indexOf(groupID);
						if(i != -1) {
							awaitingAck = false;
							comms.confirmations.remove(i);
							System.out.println("confirmation received for:" + groupID);
						}
						comms.messagesLock.unlock();
					}
					//todo - timeout if confirmation NOT received
				}
			}
		});
		stateUploader.start();
		
		Thread stateReceiver = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					//receive state from server
					if(comms.stateUpdates.size() == 0) {
						try {
							Thread.sleep(10);
							continue;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					comms.messagesLock.lock();
					compareStates(state, comms.stateUpdates);
					comms.stateUpdates.clear();
					comms.messagesLock.unlock();
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
