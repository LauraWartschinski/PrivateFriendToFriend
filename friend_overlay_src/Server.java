package friend_overlay_src;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;



/**
 * This class receives connection requests from other peers over the Friend Overlay.
 * If verifies the identity of a peer before accepting a connection and establishes a session key with the peer.
 * This class is a thread.
 */
public class Server implements Runnable {

    /**
     * Port of the Server
     */
    private int port;
    private ServerSocket MyService;
    private OrganizerThread orga;
    private Keys k;

    public Server(int p, OrganizerThread orga){
        this.port=p;
        this.orga=orga;
        this.k=new Keys();
    }

    /**
     * Creates ServerSocket for receiving connections later and updates the port.
     */
    public void createServerSocket(){

        try {
            MyService = new ServerSocket(port);
            port=MyService.getLocalPort();

        }catch (IOException e) {
            System.err.println(e);
        }

    }


    /**
     * This method listens on port of the ServerSocket for new connection requests.
     * If a connection is received, the server will verify if the sender is in the list of friends.
     * If that is the case, it will create a new PeerConnection for this peer and start it's threads.
     * It will then notify the OrganizerThread about this new PeerConnection.
     * @return true if received connection was verifyed and a PeerConnection was started.
     */
    public boolean acceptConnections() {
        // Öffnet Service auf Port und nimmt Traffic auf dem Port an

            try {
                Socket socket = MyService.accept();
                //System.out.println("FriendOverlay: Received connection request.");

                //überprüft ob der Peer bekannt ist. Falls ja, gibt Friend Object zurück.
                PrivateKey key=orga.getKey();
                Friend f=verifyPeer(socket,key);
                if( f!=null) {
                    boolean encryption=orga.UseEncryption();
                    PeerConnection pc = new PeerConnection(this.orga, f, socket, key, encryption);
                    pc.startThreads();
                    orga.getPcs().add(pc);

                }else {
                  //  System.out.println("FriendOverlay: Denied connection request of peer. Either the peer is unknown or verification failed.");
                    socket.close();
                }

            }catch (IOException e){
                e.printStackTrace();
                return false;
            }
            return true;

    }

    //Überfrüft ob Peer bekannt ist anhand dessen Public Keys. Falls ja, gibt das dazugehörige Friend-Object zurück.

    /**
     * This method reads the first message sent by a peer to the ServerSocket.
     * It then tries to decrypt the message and checks if the peer is in the list of friends.
     * If this is successful it will generate a session key and send it to the peer.
     * @param socket
     * @param key
     * @return Friend object of identified peer, null otherwise
     */
    public Friend verifyPeer( Socket socket, PrivateKey key  ){

        try {
            InputStream is= socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String newline=br.readLine();

            String decrypted=k.decrypt(newline, key);
            Friend f=whoIsThisPeer(decrypted);

            if(f!=null) {

                PrintStream output = new PrintStream(socket.getOutputStream());
                //Timestamp now = new Timestamp(System.currentTimeMillis());
                //Timestamp earlier = new Timestamp(System.currentTimeMillis());

                SecretKey skey= k.generateSymmetricKey();
                PublicKey publickey=f.getPublickey();
                String skey_string=k.keytoString(skey);

                String answer = k.encrypt("Ok "+skey_string, publickey);
                //while ((now.getTime() - earlier.getTime()) < 2) {
                output.println(answer);
                f.setSkey(skey);
                  //  now = new Timestamp(System.currentTimeMillis());

                //}

                return f;

            }else {
               // System.out.println("FriendOverlay: Public key not in list of friends.");
            }
            is.close();
            br.close();

        } catch (IOException | NullPointerException e) {
                 e.printStackTrace();

        }

        return null;
    }


    /**
     * This method returns a Friend object if the String 'key' was the start of one of the public keys of a friend in
     * the list of friends in the OrganizerThread.
     * @param key
     * @return Friend object if key was start of a key of a friend, otherwise null
     */
    public  Friend whoIsThisPeer(String key){

        List<Friend>contacts= orga.getContacts();
        for(int i=0; i<contacts.size();i++) {

            Friend f=contacts.get(i);
            String secret=k.keytoString(f.getPublickey()).substring(0,50);
            if(key.equals(secret)){

                return f;
            }

        }
        return  null;
    }

    public int getPort(){
        return this.port;
    }

    /**
     * Constantly running method of server.
     * On start it will create ad distinct and recognizable name for the thread.
     * While not interrupted, the method will check for new connections.
     * If a connection request failed, it will wait for 1000 ms before listening again.
     */
    @Override
    public void run() {

        final String orgName = Thread.currentThread().getName();
        Thread.currentThread().setName(orgName + "-ServerThread");


        //System.out.println("FriendOverlay: Server Thread running on port ".concat(Integer.toString(port)) );
        while (!Thread.currentThread().isInterrupted()) {
            boolean ok=acceptConnections();

            if(!ok){
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                   // e.printStackTrace();
                    System.out.println("FriendOverlay: Server Thread on port ".concat(Integer.toString( MyService.getLocalPort())).concat(" failed with error.") );
                }
            }

        }
    }
}
