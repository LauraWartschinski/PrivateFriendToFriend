package connection_overlay_src;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Periodically checks if less then 4 peers are connected. If so, the watcher will
 * try to open new connections using the known peer list, which gets expanded
 * over the network (see keep alive checks at {@link ConnectionHandler}).
 * 
 * Also checks at the same time, if there are multiple connections to the same address.
 * If yes, then one of these connections get shut down which each check cycle, until
 * there are no duplicates.
 * 
 */
public class ConnectionWatcher extends Thread{
	
	/**
	 * ConnectionHandler class list. Needed to address each connection individually.
	 */
	private List<ConnectionHandler> connectionHandlerList;
	/**
	 * List of listeners belonging to open connections with other peers.
	 */
	private List<String> activeConnectionList;
	/**
	 * List of known listeners/peers.
	 */
	private List<String> knownConnectionList;
	/**
	 * Tells the thread if it should shut down.
	 */
	private boolean running;
	/**
	 * ConnectionInterface class
	 */
	private ConnectionInterface connectionInterface;
	/**
	 * String representation of own listener address as "Port-IP"
	 */
	private String ownListener;
	/**
	 * Queue to all open connection threads.
	 */
	private BlockingQueue<String> senderQueue;
	
	/**
	 * Takes all information from the ConnectionInterface.
	 * @param conInteface ConnectionInterface class
	 * @param sQueue Queue to all open connection threads / ConnectionHandler classes
	 */
	public ConnectionWatcher(ConnectionInterface conInteface, BlockingQueue<String> sQueue){
		
		this.connectionHandlerList = conInteface.getConnectionHandlerList();
		this.activeConnectionList = conInteface.getActiveConnectionList();
		this.knownConnectionList = conInteface.getSuperListenerList();
		this.connectionInterface = conInteface;
		this.ownListener = connectionInterface.getOwnListener();
		this.running = true;
		this.senderQueue = sQueue;
	}
	
	//@Override
	/**
	 * Runnable method which loops until overlay is shut down.
	 * Performs duplicate connection checks and checks 
	 * if there should be more open connections. If there are less then 5 open 
	 * connections, the thread looks through the known connection list and tries to
	 * establish new connections over these.
	 * 
	 * The checks are made on intervals, since most runs the watcher will take no action.
	 * Intervals are 3 seconds apart.
	 */
	public void run(){
		while (running){
			
			// shutdown duplicate TCP connections (one at a time)
			loop: for (int i=0; i<activeConnectionList.size(); i++) {

			     for (int j=0 ; j<activeConnectionList.size(); j++) {
			          if ((activeConnectionList.get(i).equals(activeConnectionList.get(j))) && (i != j)) {
			              
			        	  for (ConnectionHandler handler : connectionHandlerList){
			        	  		System.out.println("removing double connection from: "+handler.getConnectedListener());
			        	  		if( handler.getConnectedListener().equals(activeConnectionList.get(j)) ){
			        	  			handler.shutdownConnection();
			        	  			break loop;
			        	  		}
			        	  	}
			        	  
			          }
			     }
			 }

			
			// auto connect to other known peers, if number of open connections is to low
			if (activeConnectionList.size() <= 4){
				ArrayList<String> removeList = new ArrayList<String>();
				List<String> knownConnectionList2 = new ArrayList<String>();;
				knownConnectionList2.addAll(knownConnectionList);
				
				for (String listener : knownConnectionList2){
					if ((!activeConnectionList.contains(listener)
					&& (!ownListener.equals(listener)))){
						String split[] = listener.split("-");
						
						try {
							connectionInterface.newConnection(new Socket( InetAddress.getByName(split[1]), Integer.parseInt(split[0])));
							senderQueue.add("listener " + connectionInterface.getOwnListener());
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							senderQueue.remove();
						
						// peer is out of date, remove it from known peer list
						} catch (NumberFormatException | IOException e) {
							removeList.add(listener);
						}
						
					}
				}
				// remove dead peers from known list
				if (!removeList.isEmpty()){
					for (String removeElement : removeList){
						connectionInterface.deadSuperListener(removeElement);
					}
				}
			}
			
			
			// check only needs to be made so often
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	/**
	 * Tells the watcher that the overlay wants to shut down.
	 */
	public void shutdownWatcher(){
		this.running = false;
	}
	
}
