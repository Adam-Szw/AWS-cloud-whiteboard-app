package application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Container class that allows separation of state from synchronisation.
 * This allows for saving the state in case of disconnection from the server
 * 
 * @author aks60
 *
 */
public class State {
	
	public Lock updateLock = new ReentrantLock();
	public UpdateGroup currentUpdate = new UpdateGroup();
	public Lock stateLock = new ReentrantLock();
	public List<UpdateGroup> totalState = new ArrayList<UpdateGroup>();
	public long currentStateID = 0;
	
}
