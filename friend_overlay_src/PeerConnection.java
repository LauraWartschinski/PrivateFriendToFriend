package friend_overlay_src;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.Vector;

/**
 * PeerConnection: A class for managing the connection to another friend
 */
public class PeerConnection{

    private UUID uuid;
    private ConnectionThread reciving;
    private ConnectionThread sending;
    private Thread ts;
    private Thread tr;
    private Socket  socket;

    private Friend friend;

    /**
     * PrivateKey of client. Used for decryption.
     */
    final private PrivateKey privateKey;
    final private boolean useEncryption;

    private OrganizerThread orga;

    Keys k=new Keys();

    /**
     * Constructor for a peer connection that is created when we connect to a friend
     * @param orga - friend overlay organizer thread
     * @param f - Friend we want to connect to
     * @param key - our private key
     * @param useEncryption - if we use encryption
     */
    PeerConnection(OrganizerThread orga, Friend f, PrivateKey key, boolean useEncryption){
        this.uuid = UUID.randomUUID();
        this.friend=f;
        this.orga=orga;
        this.privateKey=key;
        this.useEncryption=useEncryption;
    }

    /**
     * Constructor for a PeerConnection that is created when a friend connects to us
     * @param orga - friend overlay organizer thread
     * @param f - friend that is connecting
     * @param s - socket
     * @param key - our private key
     * @param useEncryption - if we use encryption
     */
    PeerConnection(OrganizerThread orga, Friend f, Socket s, PrivateKey key, boolean useEncryption){
        this.uuid = UUID.randomUUID();
        this.socket=s;
        this.friend=f;
        this.orga=orga;
        this.privateKey=key;
        this.useEncryption=useEncryption;

    }

    /**
     * Establish sending and receiving connection in their own threads.
     * @return true iff successful
     */
    public boolean establishConnection(){        
        boolean ok=openSocket();
        if( ok  ) {
            sending=new ConnectionThread(socket,true, this);
            reciving=new ConnectionThread(socket, false, this);
            if(sending!=null && reciving!=null){
      
                if(verificationToPeer()){
                    return true;
                }else{
/*                    System.out.println("FriendOverlay:Peer "+getPeerID()
                                        +" (Ip: "+friend.getIp()
                                        +", Port: "+Integer.toString(friend.getPort())
                                        +") denied connections. ");*/

                }
            }else{
              /*  System.out.println("FriendOverlay: Could not establish connection to Peer "+getPeerID()
                        +" (Ip: "+friend.getIp()
                        +", Port: "+Integer.toString(friend.getPort())
                        +"). ");*/
            }

        }else{
           /* System.out.println("FriendOverlay: Peer "+getPeerID()
                    +" (Ip: "+friend.getIp()
                    +", Port: "+Integer.toString(friend.getPort())
                    +") is not reachable.");*/
        }
        try {
            socket.close();
        }catch(NullPointerException | IOException e){

        }
        return false;
    }

	/**
	 * Verification to peer: send first 50 bytes of our public key to authenticate. If successful, receives "OK" encrypted with our own public key.
	 * @throws socket error, verification error
	 * @return true iff verification was successful
	 */
    private boolean verificationToPeer(){
        Keys k=new Keys();
        reciving.createInputStream();
        sending.createOutputStream();
        
        try {

            PrintStream output = sending.getOutputSteam();
            BufferedReader br=reciving.getReader();

            //senden der ersten Nachricht an Peer zur verifizierung
            String peer_pub=k.keytoString(orga.getPub());
            String secret=peer_pub.substring(0,50);
            String secret_encrypt=k.encrypt( secret ,friend.getPublickey() );
            output.println(secret_encrypt);

            Timestamp now=new Timestamp(System.currentTimeMillis());
            Timestamp earlier=new Timestamp(System.currentTimeMillis());
            while( (now.getTime() - earlier.getTime()) < 200) {

                //Empfang, dass connection akzeptiert wurde
                String newline = br.readLine();

                //System.out.println("FriendOverlay:"+socket.getPort());
                //System.out.println("FriendOverlay:"+newline);

                String msg = k.decrypt(newline, privateKey);
                String[] msg_parts=msg.split(" ");
                if(msg_parts.length==2) {

                    if(msg_parts[0].equals("Ok")){
                        //Verbindung wurde  akzeptiert

                        SecretKey skey= k.stringToSecretKey(msg_parts[1]);
                        friend.setSkey(skey);

                        startThreads();
                        return true;
                    }
                    now = new Timestamp(System.currentTimeMillis());
                }
            }

        } catch (IOException e) {
            return false;
        }catch (NullPointerException e){
            return false;
        }
        return false;
    }

    
    /**
     * If we are creating a connection (active), this is used for opening the socket
     * @return true if successful
     */
    public boolean openSocket(){
        try {
            socket = new Socket(friend.getIp(), friend.getPort());
        } catch (IOException e) {
        	//System.err.println(e);
            return false;
        }
        return true;
    }

    /**
     * If we are receiving a connection (passive), we establish it here
     */
    public void startThreads(){

        sending=new ConnectionThread(socket,true, this);
        reciving=new ConnectionThread(socket, false, this);

        if(sending!=null && reciving!=null){
            ts = new Thread(sending);
            tr=new Thread(reciving);

            ts.start();
            tr.start();
            System.out.println("FriendOverlay: Connected to peer "
                    +friend.getID()
                    +"( Ip "
                    +friend.getIp()
                    +", Port "
                    +Integer.toString(friend.getPort())
                    +").");
            

        }else{
            System.out.println("FriendOverlay: Could not establish connection to peer "
                    +friend.getID()
                    +"(IP "+ friend.getIp()
                    +", Port "
                    +Integer.toString(friend.getPort())
                    +").");

        }


    }

    /**
     * closes connections and causes the organizer threat to remove them
     */
    public void endConnections(){
        System.out.println("FriendOverlay: Closed Connection to peer "+getPeerID()+".");
        if(ts!=null){ts.interrupt();}
        if(tr!=null){tr.interrupt();}
        try {
            socket.close();
        }catch(NullPointerException | IOException e){

        }

        this.orga.removePc(this.getPeerID());
    }

    public UUID getUuid() {

        return uuid;
    }

    public String getPeerID() {
        return friend.getID();
    }

    public ConnectionThread getReciving() {
        return reciving;
    }

    public void setReciving(ConnectionThread reciving) {
        this.reciving = reciving;
    }

    public ConnectionThread getSending() {
        return sending;
    }

    public void setSending(ConnectionThread sending) {
        this.sending = sending;
    }

    public OrganizerThread getOrga() {
        return orga;
    }

    public Thread getTs() {
        return ts;
    }
    
    public Friend getFriend() {
    	return friend;
    }

    public Thread getTr() {
        return tr;
    }

    public String getIp() {
        return friend.getIp();
    }

    public void setIp(String ip) {
        friend.setIp( ip);
    }

    public int getPort() {
        return friend.getPort();
    }

    public void setPort(int port) {
        this.friend.setPort( port);
    }

    public PublicKey getPubKey() {
        return friend.getPublickey();
    }

    public boolean UseEncryption() {
        return useEncryption;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

	public SecretKey getSkey(){
        return friend.getSkey();
    }

	


	/**
	 * Sends an announcement to our friend that we are connected to, to inform him of files we are able to provide
	 * @param toAnnounce - a vector of strings that represent the filenames
	 */
	public void announce(Vector<String> toAnnounce) {
        PrintStream output = sending.getOutputSteam();

        String announcement = "announce ";
		for (int i = 0; i < toAnnounce.size(); i++) {
			announcement = announcement + " " + toAnnounce.get(i);
		}
        
        announcement=k.encryptWithSkey(announcement, friend.getSkey());
        

        output.println(announcement);      
        
	}

	/**
	 * Handling an incoming announcement message that tells us our friend can deliver files
	 * @param msg - the message
	 */
	public void receiveAnnouncements(String msg) {

		
		String[] an=msg.split(" ");
        if (an.length < 2) {
        	// apparently no files were announced?
        }
        else {
        	//start at 1, since the first entry is not an announcement but just the keyword for the transmission
        	for (int i = 1; i < an.length; i++) {
        		if (an[i].length() > 1) {
	        		orga.announceFile(an[i], friend);
        		}
        	}
        }
		
	}

	/**
	 * Handling an incoming request for a file
	 * @param msg - the file request
	 */
	public void receiveRequest(String msg) {
        String[] re=msg.split(" ");
		orga.request(re[1], this);
        
	}

	/**
	 * Sending out a request for a file to our friend
	 * @param identifier - the name of the file
	 */
	public void sendRequest(String identifier) {
		PrintStream output = sending.getOutputSteam();
		String msg = "request " + identifier;

        msg=k.encryptWithSkey(msg, friend.getSkey());
		output.println(msg);
	}

	/**
	 * Sending out a file with its contents to our friend
	 * @param file - the content of the file, starting with the filename, a space and the actual content
	 */
	public void sendFile(String file) {
		String msg = "transmit " + file;
		PrintStream output = sending.getOutputSteam();

        msg=k.encryptWithSkey(msg, friend.getSkey());
        output.println(msg);
	}
	
	/**
	 * Handling an incoming file transmit
	 * @param msg
	 */
	public void receiveTransmit(String msg) {
		
		if ( msg.startsWith("transmit ")){
			String message = msg.substring("transmit ".length());
			String head = message.split(" ")[0];
			String content = message.substring(head.length()+1);
			orga.receiveFile(head,content);
		}
	}



}
