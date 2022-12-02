package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is responsible for managing the state of the application
 * by communicating with the server. This is where 'the magic' happens essentially
 * 
 * @author aks60
 *
 */
public class Synchroniser {
	
	private Comms comms;
	private GraphicsImplementer implementer;
	private State state;
	
	public Synchroniser(State state, Comms comms, GraphicsImplementer implementer) {
		this.state = state;
		this.comms = comms;
		this.implementer = implementer;
		state.currentUpdate = new UpdateGroup();
		state.totalState = new ArrayList<UpdateGroup>();
	}
	
	public void start() {
		Thread stateUploader = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!comms.closed) {
					if(state.currentUpdate.empty) {
						App.sleepThread("State sender", App.CLIENT_TICKRATE);
						continue;
					}
					
					//send current state change
					long groupID = sendStateUpdate();
					//wait for confirmation
					confirmStateReceived(groupID);
					
					App.sleepThread("State sender", App.CLIENT_TICKRATE);
				}
			}
		});
		stateUploader.start();
		
		Thread stateReceiver = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!comms.closed) {
					//receive state from server
					comms.messagesLock.lock();
					int i = comms.stateUpdates.size();
					if(i > 0 || state.currentStateID < comms.serverStateID) {
						state.stateLock.lock();
						compareStates(state.totalState, comms.stateUpdates);
						comms.stateUpdates.clear();
						boolean awaitingAck = false;
						long id = 0;
						//Mismatch detected - fetch full history
						if(state.currentStateID < comms.serverStateID) {
							if(App.DEBUG_MODE) System.out.println(
									"Mismatch detected: " + state.currentStateID + "/" + comms.serverStateID + ", requesting full state");
							id = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
							comms.addMessage("FETCH_HISTORY;ID:" + id + ";");
							awaitingAck = true;
						}
						state.stateLock.unlock();
						comms.messagesLock.unlock();
						//Block the thread while we wait for confirmation
						while(awaitingAck) {
							comms.messagesLock.lock();
							int j = comms.confirmations.indexOf(id);
							if(j != -1) {
								awaitingAck = false;
								if(App.DEBUG_MODE) System.out.println("Full state request accepted by the server");
							}
							comms.messagesLock.unlock();
						}
					}
					else {
						comms.messagesLock.unlock();
						App.sleepThread("State receiver", App.CLIENT_TICKRATE);
					}
				}
			}
		});
		stateReceiver.start();
	}
	
	public long sendStateUpdate() {
		state.updateLock.lock();
		comms.addMessage(state.currentUpdate.toString());
		state.stateLock.lock();
		state.totalState.add(state.currentUpdate);
		state.stateLock.unlock();
		long groupID = state.currentUpdate.id;
		state.currentUpdate = new UpdateGroup();
		state.updateLock.unlock();
		return groupID;
	}
	
	public void confirmStateReceived(long id) {
		boolean awaitingAck = true;
		while(awaitingAck) {
			comms.messagesLock.lock();
			int i = comms.confirmations.indexOf(id);
			if(i != -1) {
				awaitingAck = false;
				state.currentStateID++;
				comms.confirmations.remove(i);
			}
			comms.messagesLock.unlock();
		}
		//todo - timeout if confirmation NOT received
	}
	
	public void compareStates(List<UpdateGroup> current, List<UpdateGroup> updates) {
		Collections.sort(current);
		Collections.sort(updates);
		//check for missing update IDs and implement gaps
		for(int i = 0; i < updates.size(); i++) {
			boolean found = false;
			for(int j = 0; j < current.size(); j++) {
				if(current.get(j).id == updates.get(i).id) {
					found = true;
					break;
				}
			}
			if(!found) {
				current.add(updates.get(i));
				implementer.implement(updates.get(i));
				state.currentStateID++;
			}
		}
	}

}
