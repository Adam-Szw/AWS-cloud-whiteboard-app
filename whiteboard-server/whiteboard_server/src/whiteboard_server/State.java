package whiteboard_server;

import java.util.ArrayList;
import java.util.List;

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
	
}
