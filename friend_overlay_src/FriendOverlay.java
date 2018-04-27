package friend_overlay_src;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

/**
 * Intermediate layer between OrganizerThread and MainClass.
 * Only for administrative purposes.
 */
public class FriendOverlay {

	private Scanner scanner;
    private OrganizerThread orga;
    private List<Friend> contacts;
	private String name;
	public int serverport;
	public String home;

	/**
	 * Constructor
	 */
	public FriendOverlay() {
    	
    }


	/**
	 * Reads list of contacts from file.
	 * Starts actual Overlay ('FriendOverlay') by creating and starting the OrganizerThread
	 * (which creates connections to peers and creates a server thread).
	 *
	 * @param useEncryption
	 * @param key
	 * @param pub
	 * @param home
	 * @param name
	 * @param connectionOverlayQueue
	 * @param serverport
	 * @throws IOException
	 */
	public void startOverlay(boolean useEncryption, PrivateKey key, PublicKey pub, String home, String name, BlockingQueue<String> connectionOverlayQueue, int serverport) throws IOException{

    	this.name = name;
		this.scanner = new Scanner(System.in);

         this.serverport= serverport;
         this.home=home;

        Parser p=new Parser();
        p.setCSVLocation(home);
        contacts=p.getFriendsFromFile();

        orga=new OrganizerThread(serverport, contacts, key, pub, useEncryption, home, name, connectionOverlayQueue);
        Thread orga_thread=new Thread(orga);
        orga_thread.start();
    }


	/**
	 * Lets user request a file from the available ones and calls the method request in the OrganizerThread.
	 */
	public void requestFile() {
		Vector<String> files = orga.getFiles();
		if (files.size() > 0) {

			System.out.println("What is the name of the file you want to request?");
			for (String file : files) {
				System.out.print("\""+file+"\" ");
			}
			System.out.println();
			String filename = scanner.nextLine();
			//System.out.println("Requesting: " + filename);
			
			orga.request(filename);
			
			return;

		}
		else {
			System.out.println("No files available from the friend network.\n");
			return;
		}
		
	}

	/**
	 * This method lets the user enter information about a new friend.
	 * Checks if input is valid and calls saveNewFriend().
	 */
	public void getFriendFromCommandline(){

		try {

			System.out.println("Please enter a name or a handle for this friend.");
			String name_f = scanner.next();
			while(name_f==""){
				System.out.println("The name can not be an empty String. Please enter a name or a handle for this friend.");
				name_f = scanner.next();
			}


			System.out.println("Please enter port.");
			int port_f=-1;
			while(port_f==-1) {
				try {
					port_f = scanner.nextInt();
				} catch (java.util.InputMismatchException e) {
					System.out.println("The port was not a number. If port is not known, set it to 0.");
					scanner.next();
				}
			}

			System.out.println("Please enter the ip address of the friend.");
			String ip_f = scanner.next();
			String regex="(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
			while(! (ip_f.matches(regex))){

				System.out.println("Please enter valid ip. If ip is not known, set it to '0.0.0.0'. " );
				ip_f=scanner.next();
			}


			System.out.println("Please enter public key of friend.");
			String pub_f = scanner.next();
			while(pub_f.length()!=128){
				System.out.println("This does not seem to be a valid key. Keys have a lenght of 128 characters. Please enter public key of friend.");
				pub_f = scanner.next();
			}

			this.saveNewFriend(name_f, ip_f, port_f, pub_f);


		}catch (java.util.InputMismatchException e){
			System.out.println("Please try again.");
			scanner.next();
		}
	}

	//Schreibt Freund in CSV Datei und hängt ihn an die momentan verwendete Contacs Listen in Orga und FriendOverlay an.

	/**
	 * Adds new Friend into the system by writing the information to the CSV file (by calling addFriendtoCSV in Parser).
	 * Also adds the new Friend to contacts in FriendOverlay and in OrganizerThread.
	 * @param id
	 * @param ip
	 * @param port
	 * @param publickey
	 */
	public void saveNewFriend(String id, String ip, int port, String publickey){
		Parser p=new Parser();
		p.setCSVLocation(home);
		p.addFriendtoCSV( id,  ip,  port,  publickey);

		Keys k=new Keys();
		PublicKey pub= k.StringToPublicKey(publickey);
		Friend f=new Friend(id, ip, port , pub);

		this.contacts.add(f);
		this.getOrga().getContacts().add(f);

	}

	/**
	 *
	 * @return OrganizerThread object started by this FriendOverlay.
	 */
	public OrganizerThread getOrga() {
		return orga;
	}

}
