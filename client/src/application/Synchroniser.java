package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This class is responsible for managing the state of the application
 * by communicating with the server. This is where 'the magic' happens essentially
 * 
 * @author aks60
 *
 */
public class Synchroniser {
	
	private Connector connector;
	private GraphicsImplementer implementer;
	private State state;
	
	private boolean firstTimeCheck = true;
	
	public boolean closed = false;
	
	private boolean requestingState = false;
	
	public Synchroniser(State state, Connector connector, GraphicsImplementer implementer) {
		this.state = state;
		this.connector = connector;
		this.implementer = implementer;
		state.currentUpdate = new UpdateGroup();
		state.totalState = new ArrayList<UpdateGroup>();
	}
	
	public void start() {
		Thread stateUploader = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!closed) {
					if(connector.comms.closed) {
						App.sleepThread("State sender", App.CLIENT_TICKRATE);
						continue;
					}
					if(state.currentUpdate.empty) {
						App.sleepThread("State sender", App.CLIENT_TICKRATE);
						continue;
					}
					//send current state change
					long groupID = sendStateUpdate();
					//wait for confirmation
					confirmMessageReceived(groupID, App.CLIENT_TICKRATE*100);
					
					App.sleepThread("State sender", App.CLIENT_TICKRATE);
				}
			}
		});
		stateUploader.start();
		
		Thread stateReceiver = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!closed) {
					if(connector.newComms) {
						connector.newComms = false;
						firstTimeCheck = true;
					}
					if(connector.comms.closed) {
						firstTimeCheck = true;
						App.sleepThread("State receiver", App.CLIENT_TICKRATE);
						continue;
					}
					//receive state from server
					connector.comms.messagesLock.lock();
					int i = connector.comms.stateUpdates.size();
					boolean checkMyState = i > 0 || state.currentStateID != connector.comms.serverStateID || firstTimeCheck;
					if(checkMyState) {
						state.stateLock.lock();
						compareStates(state.totalState, connector.comms.stateUpdates);
						connector.comms.stateUpdates.clear();
						long id = 0;
						boolean stateBehindServer = state.currentStateID < connector.comms.serverStateID;
						boolean stateAheadOfServer = state.currentStateID > connector.comms.serverStateID + 1;
						boolean mismatch = stateBehindServer || stateAheadOfServer || firstTimeCheck;
						//Mismatch detected - fetch full history
						if(mismatch) {
							if(App.DEBUG_MODE && !firstTimeCheck) System.out.println(
									"Mismatch detected: " + state.currentStateID + "/" +
											connector.comms.serverStateID + ", requesting full state");
							Random rd = new Random();
							id = Math.abs(rd.nextLong());
							connector.comms.addMessage("FETCH_HISTORY;ID:" + id + ";");
							requestingState = true;
						}
						state.stateLock.unlock();
						connector.comms.messagesLock.unlock();
						//Block the thread while we wait for confirmation
						if(requestingState) {
							confirmStateRequestReceived(id, App.CLIENT_TICKRATE * 100);
						}
						firstTimeCheck = false;
					}
					else {
						connector.comms.messagesLock.unlock();
						App.sleepThread("State receiver", App.CLIENT_TICKRATE);
					}
				}
			}
		});
		stateReceiver.start();
	}
	
	public long sendStateUpdate() {
		state.updateLock.lock();
		connector.comms.addMessage(state.currentUpdate.toString());
		state.stateLock.lock();
		state.totalState.add(state.currentUpdate);
		state.stateLock.unlock();
		long groupID = state.currentUpdate.id;
		state.currentUpdate = new UpdateGroup();
		state.updateLock.unlock();
		return groupID;
	}
	
	public void sendClearUpdate() {
		if(App.DEBUG_MODE) System.out.println("Sending out state clear signal");
		state.updateLock.lock();
		state.stateLock.lock();
		state.totalState.clear();
		state.currentStateID = 0;
		state.currentUpdate = new UpdateGroup();
		state.stateLock.unlock();
		state.updateLock.unlock();
		Random rd = new Random();
		long id = Math.abs(rd.nextLong());
		connector.comms.messagesLock.lock();
		connector.comms.addMessage("CLEAR;ID:" + id + ";" + "\n");
		connector.comms.serverStateID = 0;
		connector.comms.messagesLock.unlock();
		// Block everything until clear signal is received
		confirmMessageReceived(id, App.CLIENT_TICKRATE*10);
		if(App.DEBUG_MODE) System.out.println("Clear signal received by the server");
		// Trigger server match
		connector.comms.serverStateID = -1;
	}
	
	private void confirmStateRequestReceived(long id, long timeout) {
		long start = System.currentTimeMillis();
		while(requestingState) {
			long passed = System.currentTimeMillis() - start;
			if(passed > timeout) {
				if(App.DEBUG_MODE) System.out.println("Acknowledgment of state request not received from server");
				break;
			}
			connector.comms.messagesLock.lock();
			int j = connector.comms.confirmations.indexOf(id);
			if(j != -1) {
				requestingState = false;
				implementer.clear();
				state.clear();
				if(App.DEBUG_MODE) System.out.println("Full state request accepted by the server");
			}
			connector.comms.messagesLock.unlock();
		}
	}
	
	private void confirmMessageReceived(long id, long timeout) {
		boolean awaitingAck = true;
		long start = System.currentTimeMillis();
		while(awaitingAck) {
			long passed = System.currentTimeMillis() - start;
			if(passed > timeout) {
				if(App.DEBUG_MODE) System.out.println("Acknowledgment of message not received from server");
				break;
			}
			connector.comms.messagesLock.lock();
			int i = connector.comms.confirmations.indexOf(id);
			if(i != -1) {
				awaitingAck = false;
				state.currentStateID++;
				connector.comms.confirmations.remove(i);
			}
			connector.comms.messagesLock.unlock();
		}
	}
	
	private void compareStates(List<UpdateGroup> current, List<UpdateGroup> updates) {
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
	
	public void close() {
		closed = true;
	}

}
