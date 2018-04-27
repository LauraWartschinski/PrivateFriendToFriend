package friend_overlay_src;

import java.io.*;
import java.security.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

/**
 * This class administers all connections of the friend overlay.  It knows about all open connections,
 * all friends and files available for the client on the network.
 * It also can receive information from the Connection Overlay instance running on the client.
 * This class is a thread.
 */
public class OrganizerThread implements Runnable {
	/**
	 * List of all open PeerConnections.
	 */
	private List<PeerConnection> pcs = new ArrayList<PeerConnection>();

	/**
	 * List all Friends from friends.csv
	 */
	private List<Friend> contacts;

	/**
	 * List of all Friends the client is currently connected with.
	 */
	private List<Friend> friends = new ArrayList<Friend>();
	
	private HashMap<String, PeerConnection> pending = new HashMap<String, PeerConnection>();
	
	private List<String> wanted = new ArrayList<String>();

	private HashMap<String, FileToShare> Files = new HashMap<String, FileToShare>();

	private HashMap<String, PeerConnection> requests = new HashMap<String, PeerConnection>();
	private List<String> own_request = new ArrayList<String>();

	/**
	 * PrivateKey of this client. Used for decryption.
	 */
	final private PrivateKey key;

	/**
	 * PrivateKey of this client. Used for verification to other peers.
	 */
	final private PublicKey pub;

	final private boolean useEncryption;

	/**
	 * Port of ServerThread.
	 */
	private int server_port;
	private Server server;
	private Thread s_thread;

	/**
	 * Directory where files, keys and list of friends is located.
	 */
	private String home;
	private String name;


    private Keys k = new Keys();
	private int announce_timeout = 5000; // 10s
	private int friend_timeout = 10; // 0.01s
	private int request_timeout = 10;
	private int wanted_timeout = 10;
	private BlockingQueue<String> FriendQueue;

	/**
	 * Is initialized by FriendOverlay.
	 * port is usually set to 0, and will be later determined.
	 * @param port
	 * @param contacts
	 * @param key
	 * @param pub
	 * @param useEncryption
	 * @param home
	 * @param name
	 * @param connectionOverlayQueue
	 */
	public OrganizerThread(int port, List<Friend> contacts, PrivateKey key, PublicKey pub, boolean useEncryption,
			String home, String name, BlockingQueue<String> connectionOverlayQueue) {
		this.server_port = port;
		this.contacts = contacts;
		this.key = key;
		this.pub = pub;
		this.useEncryption = useEncryption;
		this.name = name;
		this.home = home;
		Files = readFiles(home);
		this.FriendQueue = connectionOverlayQueue;

	}


	/**
	 * Causes all managed PeerConnections with friends to close.
	 * @return true if successful
	 */
	public boolean goOffline() {

		// System.out.println("Closing Connections");
		Iterator<PeerConnection> iterator = pcs.iterator();
		for (int i = 0; i < pcs.size(); i++) {

			PeerConnection pc = iterator.next();
			pc.endConnections();

		}
		s_thread.interrupt();
		Thread.currentThread().interrupt();
		return true;
	}

	
	/**
	 * Starts a connection with a friend via the friend overlay. 
	 * @param f - the Friend we want to establish a connection with
	 */
	
	public void establishConnection(Friend f) {
				PeerConnection pc = new PeerConnection(this, f, key, useEncryption);
		
		boolean ok = pc.establishConnection();
		if (ok) {
			pcs.add(pc);			
			updateContacts();
		}
		
		
	}

	/**
	 *  Tries to establish a connection with every contact in our permanent friend list. Will fail for friends whose IP has changed, but if it hasn't, we are connected again right away.
	 */

	public void establish_Connections_On_StartUp() {
		for (int i = 0; i < contacts.size(); i++) {
			Friend f = contacts.get(i);
			establishConnection(f);
		}
	}

	public List<PeerConnection> getPcs() {
		return pcs;
	}

	public void setPcs(List<PeerConnection> pcs) {
		this.pcs = pcs;
	}

	/**
	 * Removes a Peer Connection with a friend from the list of active Peer Connections
	 * @param id of the Peer Connection to remove
	 */
	public void removePc(String id) {
		for (int i = 0; i < pcs.size(); i++) {
			PeerConnection this_pc = pcs.get(i);
			if (this_pc.getPeerID().equals(id)) {
				pcs.remove(i);

			}
		}
	}

	public Server getServer() {
		return server;
	}

	public int getServer_port(){return this.server_port;}

	public void setServer(Server server) {
		this.server = server;
	}

	public List<Friend> getContacts() {
		return contacts;
	}
	

	

	public void setContacts(List<Friend> contacts) {
		this.contacts = contacts;
	}

	/**
	 *
	 * @return PrivateKey of this client.
	 */
	public PrivateKey getKey() {
		return key;
	}

	/**
	 *
	 * @return PublicKey of this client.
	 */
	public PublicKey getPub() {
		return pub;
	}

	public boolean UseEncryption() {
		return useEncryption;
	}

	
	@Override
	/**
	 * Main run routine of the organizer thread that manages the friend overlay.
	 * Will handle all work on the friend overlay, including sending and receiving requests, announcing files to friends etc.
	 */
	public void run() {
		final String orgName = Thread.currentThread().getName();
		Thread.currentThread().setName(orgName + "-Organizer Thread");

		establish_Connections_On_StartUp();

		server = new Server(server_port, this);
		server.createServerSocket();

		server_port=server.getPort();

		s_thread = new Thread(server);
		s_thread.start();
		int last = 0;
		while (!Thread.currentThread().isInterrupted()) {
			int ms = (int) new Date().getTime();
			//if (ms > last) {
				//System.out.println("FriendQueue empty "+ FriendQueue.isEmpty() );

				//look for new friends
				if ((ms % friend_timeout) == 0) {
					while (!(FriendQueue.isEmpty())){
						try {
							String fs = FriendQueue.take();

							String[] split = fs.split("\\s+");
							String port = split[0];
							String ip = split[1];
							String pubkey = split[2];

							
							//System.out.println("Friend Overlay [Talking with Connection Overlay]: "+port + " " + pubkey + " " + ip);
							for(int i=0; i<contacts.size();i++) {
					            Friend f=contacts.get(i);
					            String secret = k.keytoString(f.getPublickey());
					            if(secret.startsWith(pubkey)){
					            	//System.out.println("Friend Overlay: Received Friend request from known peer.");
					            	f.ip = ip;
					            	f.port = Integer.parseInt(port);
					            	friends.add(f);
					            	establishConnection(f);
					            }

					        }	
						
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					}
				}
				
				
				//Announce Files
				if ((ms % announce_timeout) == 0) {

					//System.out.println("FriendOverlay[Organizer]: Number of connections " + inputs.size());
					//System.out.println("FriendOverlay[Organizer]: Number of available files " + Files.size());
					//System.out.println("\n");
					
					
//					System.out.println("We have " + pcs.size());
					Iterator<PeerConnection> iterator = pcs.iterator();
					for (int i = 0; i < pcs.size(); i++) {
					
						PeerConnection pc = iterator.next();
						Vector<String> toAnnounce = new Vector<String>();
						for (String key  : Files.keySet()) {
							if ((Files.get(key).getFriend() != pc.getFriend())) {
								toAnnounce.addElement(key);								
							}							
						}
						
            //System.out.println("Announcing");
						pc.announce(toAnnounce);
					}
					
				}
				
				if (ms % wanted_timeout == 0) {
					while (! own_request.isEmpty()) {
						String Identifier = own_request.get(0);

						if (Files.containsKey(Identifier)) {
							FileToShare f = Files.get(Identifier);
							if (f.own) {
								System.out.println("We already own that file.");
							}

							 else {
								Iterator<PeerConnection> iterator = pcs.iterator();
								PeerConnection provider = null;
								for (int i = 0; i < pcs.size(); i++) {
									PeerConnection p = iterator.next();
									if (p.getPeerID().equals(f.getFriend().id)) {
										provider = p;
									}
								}
								provider.sendRequest(Identifier);
								System.out.println("Friend Overlay: Requesting file \"" + Identifier + "\" from a friend.");
								own_request.remove(Identifier);
								wanted.add(Identifier);
							}
						}
					}
				}
					
				
				
				//answer requests
				
				if (ms % request_timeout == 0){

					while (!(requests.isEmpty())) {
						
						String Identifier = requests.entrySet().iterator().next().getKey();
						System.out.println(Identifier);
						if (Files.containsKey(Identifier)) {
							FileToShare f = Files.get(Identifier);
							if (f.own) {
								//System.out.println("own file");
								String filepath = f.getPath(); 
								String content = "";

								try {
									content = new Scanner(new File(filepath)).useDelimiter("\\Z").next();
									PeerConnection pc = requests.get(Identifier);
									pc.sendFile(Identifier + " " + content);
									requests.remove(Identifier);
									
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							} else {
								//System.out.println("elses file");
								
								Iterator<PeerConnection> iterator = pcs.iterator();
								PeerConnection provider = null;
								for (int i = 0; i < pcs.size(); i++) {
									PeerConnection p = iterator.next();
									if (p.getPeerID().equals(f.getFriend().id)) {
										provider = p;									
									}
								}
								provider.sendRequest(Identifier);
								System.out.println("We don't have this. Asking provider for file. ");
								pending.put(Identifier, requests.get(Identifier));
								requests.remove(Identifier);
								
							}
							
						}
						else {
							//we don't have that file - do nothing
						}
				}
			//}
			last = ms;
	
		}
	  }

	}

	/**
	 * Reads files from the home directory of our user so we know what files we can provide for other nodes.
	 * @param home - the name of the home directory of the user
	 * @return a map that uses file names to identify FileToShare Objects
	 */
	private static HashMap<String, FileToShare> readFiles(String home) {

		HashMap<String, FileToShare> Files = new HashMap<String, FileToShare>();
		if (home.length() > 0) {
		
			String path = "project_main_src/" + home;
			File f = new File(path);
			if (f.exists() && f.isDirectory()) {
				File[] files = new File(path).listFiles();
	
				for (File file : files) {
					if (file.isFile() && !(file.getName().equals("rsa.pub"))&& !(file.getName().equals("rsa.key")) && !(file.getName().equals("id.txt")) ) {
						String filepath = path + "/" +file.getName();
						String content = "";
						FileToShare toShare = null;
						
						try {
							content = new Scanner(new File(filepath)).useDelimiter("\\Z").next();
							toShare = new FileToShare(file.getName(),true,null,filepath);
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
	
						if (toShare != null) {
							Files.put(file.getName(), toShare);						
						}
						
					}
				}
	
			}
		}
		//System.out.println(Files.size() + " file[s] found.");
		return Files;
	}

	
	/**
	 * @return a Vector of all the files we own locally
	 */
	public Vector<String> getFiles() {
	
		Vector<String> files = new Vector<String>();
		
		for (String key  : Files.keySet()) {
			if (Files.get(key).own == false) {
				files.add(key);
			}
			
		}
		
		return files;
	}

	/**
	 * Announces a files availability to a friend, offering that we could transmit it if he wants this file.
	 * @param filename - the name of the file
	 * @param friend - the friend to announce to
	 */
	public void announceFile(String filename, Friend friend) {
		boolean found = false;
		for (String key  : Files.keySet()) {
			if (Files.get(key).identifier.equals(filename)){
				found = true;
				//System.out.println("We already know about that file: " + filename);
				if (Files.get(key).own == false) {
					Files.get(key).Update(friend);
				}
			}
		}
		
		if (!found) {
			System.out.println("Friend Overlay: Got announcement for new file called " + filename + ".");
			FileToShare f = new FileToShare(filename,false,friend,null);
			Files.put(filename, f);
		}
	}

	/**
	 * Handling a request for a file, made by our own user
	 * @param filename - the file the user wants to receive from the friend network
	 */
	public void request(String filename) {
		own_request.add(filename);
	}
	
	/**
	 * Handling a request from another friend
	 * @param filename - the requested file's name
	 * @param pc - the peer connection of the friend who wants to receive the file
	 */
	public void request(String filename, PeerConnection pc) {
		requests.put(filename, pc);		
	}
	
	/**
	 * Handling an incoming file and either saving it locally (if we want it for us), or sending it to the peer who requested it from us
	 * @param head - the name of the file
	 * @param content - the contents of the file
	 */

	public void receiveFile(String head, String content) {
		
		if (wanted.contains(head)) {			
			System.out.println("Friend Overlay: Received file " + head);
			String path = "project_main_src/" + home + "/" + head;
		    BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(path));
	
			    try {
					writer.write(content);
				    writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if (pending.containsKey(head)) {
			System.out.println("Friend Overlay: received file + " + head + " for a friend.");
			PeerConnection pc = pending.get(head);
			pc.sendFile(head + " " + content);
			pending.remove(head);		
		}
		
		
	}
	
	/**
	 * Updating the contact info stored in a file, especially IPs and Ports.
	 */
	public void updateContacts() {
		
		//System.out.println("update contacts!");
		String header="id,ipaddress,port,publickey\n";

		String filename= "project_main_src/.friends.csv";

        try {
            	File file = new File(filename);
        		file.createNewFile();
	            FileWriter fw=new FileWriter(filename);
	            BufferedWriter bufferedWriter=new BufferedWriter(fw);
	            bufferedWriter.write(header);
	            

	    		for (int i = 0; i < contacts.size(); i++) {
	    			Friend f = contacts.get(i);
	    			String s = f.id + "," + f.ip + "," + f.port + "," + k.keytoString(f.publickey);
	    			bufferedWriter.write(s);
	    		}
	            
	            try{
	                if(bufferedWriter!=null){
	                    bufferedWriter.close();
	                }
	                if(fw!=null){
	                    fw.close();
	                }

	            }catch (IOException e2){
	                e2.printStackTrace();
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

		
	
}
