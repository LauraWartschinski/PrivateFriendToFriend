package connection_overlay_src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.BlockingQueue;

import friend_overlay_src.Keys;

/**
 * Each ConnectionHandler works on one socket connection to another peer.
 * It checks if the other peer send a message or if there is a message to be send.
 * The handler also periodically requests keep alive messages and keeps track of a
 * time out timer. 
 * Together with the keep alive messages, the peer exchanges lists of known peer
 * connections/listeners.
 *
 */
public class ConnectionHandler implements Runnable{
	
	/**
	 * Tells the thread, if it should shut down.
	 */
	private boolean running;
	/**
	 * Open socket, on which the thread works.
	 */
	private Socket connection;
	/**
	 * Input buffer from the connected peer.
	 */
	private BufferedReader connectionInput;
	/**
	 * Output buffer to the connected peer.
	 */
	private PrintWriter connectionOutput;
	/**
	 * Queue coming from other classes, telling the handler what to write to the connected peer.
	 */
	private BlockingQueue<String> senderQueue;
	/**
	 * ConnectionInterface class
	 */
	private ConnectionInterface conInterface;
	/**
	 * Privatekey to decrypt friend requests.
	 */
	private PrivateKey privateKey;
	
	private PublicKey publicKey;
	/**
	 * Keys class used to decrypt messages.
	 */
	private Keys keyClass;
	/**
	 * Time stamp to check for timeouts.
	 */
	private long startTime;
	/**
	 * Current time, refreshed every thread cycle.
	 */
	private long currentTime;
	/**
	 * Tells the worker thread, if the keep alive message has been answered.
	 */
	private boolean keepaliveFlag;
	/**
	 * Address of the connected peers listener.
	 */
	private String connectedListener;
	/**
	 * Queue to the friend overlay for friend requests.
	 */
	private BlockingQueue<String> connectionOverlayQueue;
	
	/**
	 * Creates the buffered reader and writer on the given socket to communicate
	 * with the connected peer.
	 * @param socket_connection given socket to other peer
	 * @param sQueue senderQueue telling the handler what to write
	 * @param overlayQueue outgoing queue to the friend overlay
	 * @param connectionInterface ConnectionInterface class
	 * @param priKey private key
	 * @throws IOException
	 */
	public ConnectionHandler(Socket socket_connection, BlockingQueue<String> sQueue, BlockingQueue<String> overlayQueue,
			ConnectionInterface connectionInterface, PrivateKey priKey, PublicKey pubkey) throws IOException{
	
		this.connection = socket_connection;
		this.connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		this.connectionOutput = new PrintWriter(connection.getOutputStream(), false);
		this.running = true;
		this.senderQueue = sQueue;
		this.connectionOverlayQueue = overlayQueue;
		this.conInterface = connectionInterface;
		this.privateKey = priKey;
		this.publicKey = pubkey;
		this.keyClass = new Keys();
		this.keepaliveFlag = false;
	}
	
	//@Override
	/**
	 * Runnable method loops thread.
	 * Checks if the other peer has send a message and reacts depending on the content:
	 * - listener/listenerRe: Exchanging listener addresses
	 * - friendReq: Checks if own private key can decrypt request. If not it passes it to all other connections.
	 * - keepaliveReq/keepaliveAns: Exchanging keep alive messages to prevent unnoticed timeouts.
	 * 								Here the peers also exchange known open listeners, so they can form new connections.
	 * 
	 * The handler checks every thread cycle, if the ConnectionInterface wants to close
	 * the overlay. If yes, the thread loop doesn't start anew.
	 */
	public void run(){
		
		String input;
		System.out.println("Connection Overlay: Started new connection thread.");
		String output = "ignore";
		startTime = System.currentTimeMillis();
		while(running){
			try {
				// check if there is something to read				
				if (connectionInput.ready()){
					input = connectionInput.readLine();
					String[] messageParts = input.split("\\s+");
					
					switch(messageParts[0]){
					
					// receiving the listener address from a new connection
					// also sends back own listener address
					case "listener":
						connectedListener = messageParts[1];
						String answ = "listenerRe " + conInterface.getOwnListener();
						connectionOutput.print(answ+"\n");
						connectionOutput.flush();
						
						if(!conInterface.getActiveConnectionList().contains(messageParts[1])){
							conInterface.newSuperListener(messageParts[1]);
							conInterface.newActiveConnection(messageParts[1]);
						}
						break;
						
					// listener return message
					// holds the listener address of connected peer
					case "listenerRe":
						connectedListener = messageParts[1];
						if(!conInterface.getActiveConnectionList().contains(messageParts[1])){
							conInterface.newSuperListener(messageParts[1]);
							conInterface.newActiveConnection(messageParts[1]);
						}
						break;
					
					// request new connection to friend overlay
					case "friendReq":	
						// am I this friend? try to decrypt message with private key	
						String keyWord = "";
						try {
							 keyWord = keyClass.decrypt(messageParts[2], privateKey);
						}
						catch (Exception e) {
							// decryption error - we are not the intended receiver
						}
						
						if (keyWord.equals("helloworld")){
							System.out.println("Connection Overlay: Got a friend connection request");
							String metaData = keyClass.decrypt(messageParts[3], privateKey);
							/*
							String[] split = input.split("\\s+");
							String p = split[2];
							System.out.println("pubkey of friend: " + p);
							System.out.println("Our pubkey: " + keyClass.keytoString(publicKey));
						*/
							connectionOverlayQueue.add(metaData);

						// TTL to low
						} else if (Integer.parseInt(messageParts[1]) == 1){
							// do nothing
							
						// flood to others; TTL-1 
						// The thread sleep also prevents sending the message back on the way it came
						} else {
							int newTTL = Integer.parseInt(messageParts[1]) - 1;
							senderQueue.add("friendReq " + Integer.toString(newTTL) + " " + messageParts[2] + " " + messageParts[3]);
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							senderQueue.remove();
							
						}
						break;
					
						
					// keep alive request
					// also sends known open peers to the connected partner
					case "keepaliveReq":
						String answer = "keepaliveAns ";
						for (String conn : conInterface.getSuperListenerList()){
							answer = answer+" "+conn;
						}
						connectionOutput.println(answer);
						connectionOutput.flush();
						break;
					
					// keep alive answer
					// reads new open peers from the answer and adds them to own list
					case "keepaliveAns":
						int len = messageParts.length;
						for(int i=1; i<len; i++){
							if (!conInterface.getSuperListenerList().contains(messageParts[i])){		
								System.out.println("Connection Overlay: Found new connection " + messageParts[i]);
								conInterface.newSuperListener(messageParts[i]);
								
							}
						}
						keepaliveFlag = false;
						startTime = System.currentTimeMillis();
						break;
					
					// print request for debugging purposes
					case "print":			
						System.out.println(messageParts[1]);
						break;
					
					// unknown request
					default: 
						//System.out.println("Connection Overlay: Unknown message: "+messageParts[0]);
						break;
							
					}
					
				}
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
			// check if there is something to write
			// output String needs to be reset when queue is empty, else you couldn't send the same message in succession
			if(senderQueue.peek() == null){
				output = "ignore";
			}
			else if (!output.equals(senderQueue.peek())) {
				output = senderQueue.peek();
				connectionOutput.print(output + "\n");
				connectionOutput.flush();
			}
			
			// close thread and connection
			if((conInterface.getRunning() == false) || (this.running == false) || (this.connection.isClosed())){
				this.running = false;
				this.connectionOutput.close();
				conInterface.deadConnection(connectedListener);
				conInterface.deadSuperListener(connectedListener);
				try {
					this.connectionInput.close();
					this.connection.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
			// keep alive timing checks
			currentTime = System.currentTimeMillis();
	       
			// connection timeout
			if(keepaliveFlag && ((currentTime - startTime) >= 10000)){
				conInterface.deadConnection(connectedListener);
				conInterface.deadSuperListener(connectedListener);
				System.out.println("Connection Overlay: Connection to " +connectedListener+ " timed out.");
				try {
					connection.close();
					connectionInput.close();
					connectionOutput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				running = false;
			}
			
			// wait (10) seconds till next keepalive
			// TODO: set to ~1 min
			else if (!keepaliveFlag && ((currentTime - startTime) >= 10000)) {
				// send keepalive, reset clock for new timeout-countdown
				keepaliveFlag = true;
				connectionOutput.print("keepaliveReq\n");
				connectionOutput.flush();
				startTime = System.currentTimeMillis();
				
			}
		}
	}
	
	/**
	 * Gets the listener address of the connected peer.
	 * Used by the ConnectionWatcher @link {@link ConnectionHandler}, to check for duplicate connections.
	 * @return listener address of the connected peer as "Post-IP" String
	 */
	public String getConnectedListener(){
		return this.connectedListener;
	}
	
	/**
	 * Method to shutdown the connection from outside.
	 * Used by the ConnectionWatcher @link {@link ConnectionHandler} to terminate duplicate connections.
	 */
	public void shutdownConnection(){
		this.running = false;
	}
	
	
}
