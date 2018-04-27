package connection_overlay_src;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ShutdownChannelGroupException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import friend_overlay_src.Friend;
import friend_overlay_src.Keys;
import friend_overlay_src.Parser;

/**
 * ConnectionInterface is the main class of the connection overlay.
 * It initializes the Listener and ConnectionWatcher over its Constructor 
 * and a ConnectionHandler for each connecting peer using a function.
 * It also shuts down all these classes, if called to close the overlay.
 * @see #closeConnectionOverlay()
 * 
 * Most attributes of the overlay can be accessed over this class.
 * 
 */
public class ConnectionInterface{
	
	/**
	 * Tells other threads, if the Interface wants to shutdown the overlay.
	 */
	private boolean running;
	/**
	 * Handles all peer connection threads.
	 */
	private ExecutorService executor;
	/**
	 * Handles user input.
	 */
	private Scanner scanner;
	/**
	 * Queue to open peer connections. What gets passed over this queue will be send to all connected peers.
	 */
	private BlockingQueue<String> senderQueue;
	/**
	 * List of all known active peers (meaning their listener address).
	 */
	private List<String> superListenerList;
	/**
	 * List of all connected peers (meaning their listener address).
	 */
	private List<String> activeConnectionList;
	/**
	 * String representation of own listener port.
	 */
	private String listenerPort;
	/**
	 * String representation of own listener IP.
	 */
	private String listenerIp;
	/**
	 * String representation of both port and IP as "Port-IP".
	 */
	private String ownListenerStr;
	/**
	 * Listener class, which waits for new connecting peers.
	 */
	private Listener listener;
	/**
	 * Own private key to decrypt messages.
	 */
	private PrivateKey privateKey;
	/**
	 * Own public key to send to friends.
	 */
	private PublicKey publicKey;
	/**
	 * Key class, which is used to encrypt/decrypt messages.
	 */
	private Keys keyClass;
	/**
	 * Queue which connects to the friend overlay. Used to send accepted friend requests to friend overlay.
	 */
	private BlockingQueue<String> connectionOverlayQueue;
	/**
	 * ConnectionWatcher class, which watches over all open connections, trying to open new ones or remove duplicate connections.
	 */
	private ConnectionWatcher connectionWatcher;
	/**
	 * List of all ConnectionHandlers. Used to address individual peer connections, since the threads are in a dynamic thread pool (executor).
	 */
	private List<ConnectionHandler> connectionHandlerList;
	/**
	 * Port of the friend overlay listener.
	 */
	private int friendOverlayPort;

	private String home;
	private String Parser;

	/**
	 * Constructor, which also initializes the Listener and ConnectionWatcher class.
	 * 
	 * Creates lists of known open peer listeners and listeners 
	 * to which there is an active connection.
	 * 
	 * Creates a LinkedQueue which connects to the ConnectionHandler classes.
	 * Everything passed over this Queue, gets send to every other peer.
	 * 
	 * @param priKey own private key
	 * @param pubKey own public key
	 * @param overlayQueue queue connected to the friend overlay
	 * @param friendPort listener port of the friend overlay
	 * @param networkAddress IP of the network in which the overlay should work
	 * @throws IOException 
	 */
	public ConnectionInterface(PrivateKey priKey, PublicKey pubKey, BlockingQueue<String> overlayQueue, int friendPort, String networkAddress) throws IOException{
		this.listener = new Listener(this, networkAddress);
		this.listenerPort = Integer.toString(listener.getPort());
		this.listenerIp = listener.getIP();
		this.listener.start();
		this.ownListenerStr = listenerPort + "-" + listenerIp; 
		this.privateKey = priKey;
		this.publicKey = pubKey;
		this.connectionOverlayQueue = overlayQueue;
		this.executor = Executors.newCachedThreadPool();
		this.running = true;
		this.scanner = new Scanner(System.in);
		this.keyClass = new Keys();
		this.senderQueue = new LinkedBlockingQueue<String>();
		this.superListenerList = new ArrayList<String>();
		this.activeConnectionList = new ArrayList<String>();
		this.connectionHandlerList = new ArrayList<ConnectionHandler>();
		this.newSuperListener(ownListenerStr);
		this.friendOverlayPort = friendPort;
		this.connectionWatcher = new ConnectionWatcher(this, senderQueue);
		this.connectionWatcher.start();
	}

	/**
	 * 
	 * @param port
	 * @param ipStr
	 */
	public void connectToNetworkViaGUI(int port, String ipStr){
		try {
			newSuperListener(port + "-" + InetAddress.getByName(ipStr).getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Asks for the port and IP of an active peer. This connection will then 
	 * be added to the list of known active peers and the ConnectionWatcher will
	 * try to connect to it.
	 * If the Connection is successful, the program will then automatically connect 
	 * to more peers in the network (if available).
	 * 
	 */
	public void connectToNetwork(){
		try{
			// Manual connection to a listener in the connection overlay
			System.out.println("Connection Overlay: Please enter new connection port. \n");
			int port = scanner.nextInt();
			System.out.println("Connection Overlay: Please enter new connection IP.\n");
			String ipStr = scanner.next();
			
			// connecting to yourself is pointless
			if ((Integer.toString(port).equals(listenerPort)) && ( (ipStr.equals(this.listenerIp) || (ipStr.startsWith("127."))) )){
				System.out.println("Connection Overlay: You can't connect to yourself.\n");
			} else {
				newSuperListener(port + "-" + InetAddress.getByName(ipStr).getHostAddress());
			}
		} catch (UnknownHostException e) {
			System.out.println("Connection Overlay: Couldn't find the specified host.\n");
			//e.printStackTrace();
		} catch(java.util.InputMismatchException e) {
			System.out.println("Connection Overlay: Invalid Input.\n");
			scanner.next();
		}
		
	}
	
	/**
	 * 
	 * @param friendID
	 * @param contacts
	 * @param keystring
	 */
	public void sendFriendRequestViaGui(int friendID, List<Friend> contacts, String keystring){
		String keyWord = keyClass.encrypt("helloworld", contacts.get(friendID).getPublickey());
		String metaData = keyClass.encrypt(this.friendOverlayPort + " " + this.listenerIp + " " + keystring.substring(0,30), contacts.get(friendID).getPublickey());

		senderQueue.add("friendReq " + "1 " + keyWord +" "+ metaData);


		// give every thread time to read the queue entry (flood over every peer)
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		senderQueue.remove();
	}
	
	/**
	 * Shows known friends and asks to which an connection should be established.
	 * After that the request will be encrypted with the friends public key and
	 * flooded through the overlay.
	 */
	public void sendFriendRequest(){
		try{
			String keystring = new Keys().keytoString(publicKey);
			Parser parser = new Parser();
			parser.setCSVLocation(home);
			List<Friend> contacts = parser.getFriendsFromFile();
			System.out.println("Connection Overlay: Which friend do you want to send to?");
			
			for(int i=1; i<=contacts.size(); i++){
				System.out.println(i +" - "+ contacts.get(i-1).toString());
			}
			
			int friendID = scanner.nextInt()-1;
			
			String keyWord = keyClass.encrypt("helloworld", contacts.get(friendID).getPublickey());
			String metaData = keyClass.encrypt(this.friendOverlayPort + " " + this.listenerIp + " " + keystring.substring(0,30), contacts.get(friendID).getPublickey());
			
			senderQueue.add("friendReq " + "10 " + keyWord +" "+ metaData);
			
			// give every thread time to read the queue entry (flood over every peer)
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			senderQueue.remove();
		} catch (IndexOutOfBoundsException e){
			System.out.println("Connection Overlay: Input not valid. \n");
		}
	}
	
	/**
	 * Sends a test signal to all connected peers.
	 * If successful, the other peer will output "test".
	 */
	public void testSignal(){
		senderQueue.add("print test");
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		senderQueue.remove();
	}
	
	/**
	 * Starts a new ConnectionHandler (@see ConnectionHandler), 
	 * passing down a queue over which messages to other peers are send and
	 * a queue to the friend overlay (for friend requests).
	 * To handle friend requests the Handler also gets passed down the private and public key.
	 * @param new_connection Socket, which connects to another peer.
	 * @throws IOException
	 */
	public void newConnection(Socket new_connection) throws IOException{
		ConnectionHandler newHandler = new ConnectionHandler (new_connection, senderQueue, connectionOverlayQueue, this, privateKey, publicKey);
		connectionHandlerList.add(newHandler);
		executor.execute( newHandler );
	}
	
	/**
	 * Adds a new address to the active peer list.
	 * @param newListenerPort listener address in the form of "Port-IP"
	 */
	public void newActiveConnection(String newListenerPort){
		activeConnectionList.add(newListenerPort);
	}
	
	/**
	 * Removes an address from the active peer list.
	 * @param deadListener listener address in the form of "Port-IP"
	 */
	public void deadConnection(String deadListener){
		activeConnectionList.remove(deadListener);
	}
	
	/**
	 * Gets active peer connections (listener addresses).
	 * @return active peer list
	 */
	public List<String> getActiveConnectionList(){
		return this.activeConnectionList;
	}
	
	/**
	 * Adds a new address to the known peer list.
	 * @param newListenerPort listener address in the form of "Port-IP"
	 */
	public void newSuperListener(String newListenerPort) {
		superListenerList.add(newListenerPort);
	}
	
	/**
	 * Removes an address from the known peer list.
	 * @param deadListener listener address in the form of "Port-IP"
	 */
	public void deadSuperListener(String deadListener){
		superListenerList.remove(deadListener);
	}
	
	/**
	 * Gets known peer list (listener addresses).
	 * @return known peer list
	 */
	public List<String> getSuperListenerList(){
		return this.superListenerList;
	}
	
	/**
	 * Shuts down the entire connection overlay, setting 'running' in all 
	 * threads to false by setting own running variable to false.
	 * Waits for all threads to run a final time or forces shutdown after a while.
	 * @throws InterruptedException 
	 */
	public void closeConnectionOverlay() throws InterruptedException{
		running = false;
		scanner.close();
		connectionWatcher.shutdownWatcher();
		try {
			this.listener.closeListener();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		executor.shutdown(); 
		if(executor.awaitTermination(10, TimeUnit.SECONDS)){
			System.out.println("Connection Overlay: Shutting down Connection Overlay.");
		} else {
			System.out.println("Connection Overlay: Forcing Connection Overlay to shut down.");
			executor.shutdownNow();
		}
		
	}
	
	/**
	 * Returns if the interface wants to shut down.
	 * @return boolean which tells the caller if the Interface wants to shut down the overlay
	 */
	public boolean getRunning(){
		return this.running;
	}
	
	/**
	 * Returns address of own listener ("Port-IP").
	 * @return address string as "Port-IP"
	 */
	public String getOwnListener(){
		return this.ownListenerStr;
	}
	
	/**
	 * Returns all active ConnectionHandler objects.
	 * This function allows the ConnectionWatcher to address each peer connection individual.
	 * @return list of all ConnectionHandlers
	 */
	public List<ConnectionHandler> getConnectionHandlerList(){
		return this.connectionHandlerList;
	}
	
	/**
	 * Gets port of the friend overlay.
	 * @return friend overlay port as int 
	 */
	public int getFriendOverlayPort(){
		return this.friendOverlayPort;
	}
	
	/**
	 * Gets only the IP of own listener.
	 * @return listener IP as string
	 */
	public String getListenerIp(){
		return this.listenerIp;
	}

	/**
	 * Gets only the port of own listener.
	 * @return listener port as string
	 */
	public String getListenerPort(){
		return  this.listenerPort;
	}

	public void setHome(String home){
		this.home=home;
	}
}
