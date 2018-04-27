package connection_overlay_src;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * The listener class watches a specified port and waits for other peers to connect
 * over this port. If an connection is established, the listener gives the socket to 
 * a new connectionHandler, by calling a connectionInterface function.
 * The listener gets his own port, but needs to known on which connected network 
 * it should operate, by giving it the IP which the computer has in that network.
 *
 */
public class Listener extends Thread{
	
	/**
	 * Listener port.
	 */
	private int port;
	/**
	 * Listener IP as String.
	 */
	private String ip;
	/**
	 * Listener ServerSocket.
	 */
	private ServerSocket listener;
	/**
	 * Checks if it should shutdown.
	 */
	private boolean running;
	/**
	 * ConnectionInterface class, over which the listener creates a new ConnectionHandler when another peer connects.
	 */
	private ConnectionInterface connectionInterface;
	
	/**
	 * Starts the ServerSocket on a port, given by the operating system.
	 * Outputs IP and port, so the user can give it to other peers.
	 * @param connInterface ConnectionInterface class
	 * @param networkAddress IP address the computer has in the network the listener should operate
	 */
	public Listener(ConnectionInterface connInterface, String networkAddress) {
		try { 
			this.listener = new ServerSocket(0);
			this.port = listener.getLocalPort();
			this.ip = networkAddress;
			//this.ip = InetAddress.getLocalHost().getHostAddress();
			this.running = true;
			this.connectionInterface = connInterface;
			
			System.out.println("Connection Overlay: Server thread runnning on port " + this.port);
			System.out.println("Connection Overlay: Local IP is " + ip);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//@Override
	/**
	 * Runnable method, which waits for a new connection and calls the 
	 * newConnection function ({@link ConnectionInterface}) to create a ConnectionHandler
	 * that works on the new socket / connection.
	 * The thread ends, when closeListener is called from outside this class
	 * (since the accept() is permanently blocking).
	 */
	public void run(){
		while(running){
			try {
				// listens for other peers who want to connect to us
				connectionInterface.newConnection(listener.accept());
				//System.out.println("Connection Overlay: New peer tries to connect.");

			} catch (IOException e) {
				// this exceptin mainly gets thrown, when shutting down, see closeAll()
				// e.printStackTrace();
			}	
		}
	}
	
	/**
	 * Closes the ServerSocket and ends the listener thread.
	 * Needs to be called from the ConnectionInterface, since the listener thread is permanently blocked.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void closeListener() throws IOException, InterruptedException{
		this.running = false;
		this.listener.close();
	}
	
	/**
	 * Gets the listener port.
	 * @return listener port as integer
	 */
	public int getPort(){
		return this.port;
	}
	
	/**
	 * Gets the listener IP.
	 * @return listener IP as String
	 */
	public String getIP(){
		return this.ip;
	}
		
	
}
