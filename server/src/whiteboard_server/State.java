package whiteboard_server;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the state of an application in form of unordered list of updates
 * that if implemented would result in such state. Updates are strings
 * that can be decoded somewhere else.
 * 
 * @author aks60
 *
 */
public class State {

	public List<String> updates;
	public long stateID = 0;
	
	public State() {
		this.updates = new ArrayList<String>();
	}
	
	public void updateState(String message) {
		updates.add(message);
		stateID++;
	}
	
	public void clear() {
		updates.clear();
	}
	
	public void clearUpdate() {
		updates.clear();
		stateID = 0;
	}
	
}
