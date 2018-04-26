package friend_overlay_src;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This Class is creates a Thread for connecting with a single peer. It can only be a sender or a receiver. There are two of
 * these Threads per peer (one as sender, one as receiver) .
 * Both threads belong to a PeerConnection.
 */
public class ConnectionThread implements Runnable  {

    private Socket socket;
    private PrintStream output=null;
    private InputStream is=null;
    private BufferedReader br=null;
    private UUID uuid;
    private Timestamp last_alive_msg;
    PeerConnection pc;
    Keys k;

    /**
     * Sets function of Connection Thread. If True, this instance acts a sender, else as reciever thread.
     */
    public boolean sending;

    /**
     * Constructor of Connection Thread. Has to be initalized with a Socket, a function (Sending or not sending) and a PeerConnection object
     * which stores information about the connection with the associated peer.
     * @param s
     * @param funktion
     * @param pc
     */
    public ConnectionThread(Socket s, boolean funktion, PeerConnection pc){
        this.uuid = UUID.randomUUID();
        socket=s;
        sending=funktion;
        this.pc=pc;
        this.k=new Keys();
    }

    /**
     * Creates a Printstream for writing on the socket and sending messages to the peer.
     */
    public void createOutputStream(){
        try {
            output = new PrintStream(socket.getOutputStream());
        }
        catch (IOException  | NullPointerException e) {
            e.printStackTrace();
        }

    }

    //zum lesen reinkommender Nachrichten

    /**
     * Creates Inputstream and BufferedReader for reading Strings from the socket and receive messages sent by the peer.
     */
    public void createInputStream() {
        try {

            //BUffered reader und inputstreamreader für String, Inputstream für byteströme
            this.is= socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        }
        catch (IOException | NullPointerException e) {
            e.printStackTrace();

        }
    }

    //wird aufgerufen, wenn ConnectionThread die Sendefunktion übernimmt.

    /**
     * This method is called if sending is set to true (so if the connection thread is a sender).
     * Every 10 to 50 seconds it sends encrypted Alive messages to keep the connection open.
     * The messages are encrypted with a session key (of class 'SecretKey').
     * If an exception arises, all connections to this peer are closed.
     */
    private void sendingMode(){
        try {

            Timestamp now=new Timestamp(System.currentTimeMillis());
            long diff= now.getTime() - last_alive_msg.getTime();

            int randomNum = ThreadLocalRandom.current().nextInt(10, 50 + 1);
            if(diff > randomNum) {
                String msg="Alive";
                if(pc.UseEncryption()){
                    msg=k.encryptWithSkey(msg, pc.getSkey());
                }

                output.println(msg);
                last_alive_msg = new Timestamp(System.currentTimeMillis());
                output.flush();
                //System.out.println("FriendOverlay[Sent to "+pc.getPeerID()+ "]: Alive message.");

            }
            //TODO: Timer neu setzen wenn nachricht versendet wurde
        }catch ( NullPointerException e){
           // System.err.println(e);
            if(!Thread.currentThread().isInterrupted()) {
                pc.endConnections();
            }
        }
    }


    /**
     * This method is called if sending is set to false (so if the connection thread is a receiver).
     * It constantly checks if it received a message and tries o decrypt them with the session key.
     * If a messages starts with 'announce','transmit', 'request' the  associated methods in PeerConnection are called.
     * If no message was received for a specific time (50 ms) all connections to this peer are closed.
     * If an exception arises, all connections to this peer are closed.
     */
    private void receivingMode(){
        String msg;
        try {
            msg=br.readLine();
            //System.out.println("Some message received! " + msg);
            if(msg!=null){

                if(pc.UseEncryption()){
                    msg=k.decryptWithSkey(msg, pc.getSkey());
                    //msg=k.decrypt(msg, pc.getPrivateKey() );
                }

                //System.out.println( "FriendOverlay [recived from "+pc.getPeerID()+"]: "+msg);
                last_alive_msg = new Timestamp(System.currentTimeMillis());

                /*if(!msg.equals("Alive") && !msg.equals("OK")) {
                    System.out.println( "FriendOverlay [recived from "+pc.getPeerID()+"]: "+msg);

                }*/

                if (msg.startsWith("announce")) {
                	//System.out.println("Got announcement: " + msg);
                	pc.receiveAnnouncements(msg);
                }
                
                if (msg.startsWith("transmit")) {
                	System.out.println("Got transmit: \n" + msg);
                	pc.receiveTransmit(msg);
                }
                
                if (msg.startsWith("request")) {
                	System.out.println("Got request: " + msg);
                	pc.receiveRequest(msg);
                }
                
                
            }

            //Schliest verbindung zu Peer wenn keine NAchrichten kamen für x sekunden
            Timestamp now=new Timestamp(System.currentTimeMillis());
            long diff=now.getTime() - last_alive_msg.getTime();
            if( diff > 50) {
                System.out.println("FriendOverlay [peer "+ pc.getPeerID()+"]: Connection to peer was closed due to inactivity. ");
                pc.endConnections();

            }
        } catch (IOException | NullPointerException e) {
            //System.err.println(e);
            if(!Thread.currentThread().isInterrupted()) {
                pc.endConnections();
            }

        }
    }

    /**
     * Returns outputsteam of this ConnectionThread.
     * @return PrintStream object of ConnectionThread
     */
    public PrintStream getOutputSteam() {
        return output;
    }

    /**
     * Returns BufferedReader of this ConnectionThread.
     * @return BufferedReader object of ConnectionThread
     */
    public BufferedReader getReader() {
        return br;
    }


    @Override
    /**
     * Starts ConnectionThread. Gives Thread a unique and recognizable name.
     * Calls createOutputStream() and createInputStream() if the associated streams are not yet initialized.
     * Thread runs until it is interrupted from the outside.
     * If sending is true it constantly calls sendingMode(), else it will always calls receivingMode().
     */
    public void run() {
        final String orgName = Thread.currentThread().getName();
        Thread.currentThread().setName(orgName + "-PeerConnection:"+this.pc.getPeerID()+"-sending:"+sending);

        if(sending && output==null) {
            createOutputStream();
        }else if(br==null){
            createInputStream();
        }

        while (!Thread.currentThread().isInterrupted()) {
            last_alive_msg=new Timestamp(System.currentTimeMillis());
            if(sending) {

                sendingMode();
            }else {
                receivingMode();
            }
        }
    }
}
