package project_main_src;

import connection_overlay_src.ConnectionInterface;
import friend_overlay_src.FileToShare;
import friend_overlay_src.Friend;
import friend_overlay_src.FriendOverlay;
import friend_overlay_src.Keys;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Main class that calls all other classes needed to create COnnection Overlay
 * and Friend Overlay. 
 *
 */
public class MainProgram {
	
	/**
	 * Main method, which calls all other classes and connects them.
	 * Allows user to take one of 3 already existing identities.
	 * Sets home directory for this client in src/project_main_src.
	 * Reads keys from file or initializes new ones.
	 * Allows user to interact with program via commandline or GUI.
	 * Takes user input in main loop.
	 * @param args command line arguments
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {


		Scanner scanner = new Scanner(System.in);


        //User can select one of three identities
        System.out.println("User-ID (1, 2 or 3)");
        int input = 0;
    	try {
            input = scanner.nextInt();
        }catch (java.util.InputMismatchException e){
            scanner.next();

        }
    	String home = "";
        
    	if (input == 1) {
    		home = "user1";
    	}
    	else if (input == 2) {
    		home = "user2";
    	}
    	else if (input == 3) {
    		home = "user3";
    	}
    	else if (args.length >0) {
    		home = args[0];
    	}

        
        //sets home directory in src/project_main_src
        boolean useEncryption=true;
        String keyfile="";
        String pubfile ="";
        String name = "";
    	if (home.length() > 0) {
    		pubfile = "project_main_src/" + home + "/rsa.pub";
    		keyfile = "project_main_src/" + home + "/rsa.key";
    		
			String path = "project_main_src/" + home + "/id.txt";
			File f = new File(path);
    			if (f.exists() && !f.isDirectory()) {
    						try {
    							Scanner s = new Scanner(new File(path)).useDelimiter("\\Z");
    							name =  s.next();
    							s.close();
    						} catch (FileNotFoundException e) {
    							e.printStackTrace();
    						}
    						System.out.println("Main: " + name + " started program");
    			
    		}
    		
    		
    	}

    	//initializes key pair if useEncryption is true
		Keys k = new Keys();
		PrivateKey key = null;
		PublicKey pub = null;
        if(useEncryption) {


            if(keyfile!="") {
                k.setFilePriv(keyfile);
            }
            if(pubfile!="") {
                k.setFilePub(pubfile);
            }
            k.initializeKeys();
            key=k.getPrivateKey();
            pub=k.getPublicKey();
        }


		//initialize Queue for communication between the two overlays
		BlockingQueue<String> connectionOverlayQueue = new LinkedBlockingQueue<String>();

        //Start friend overlay

		//port for server is set to 0, so that the system selects a free port itself
		int serverport=0;
		FriendOverlay friendOverlay = new FriendOverlay();
		friendOverlay.startOverlay(useEncryption, key, pub, home, name, connectionOverlayQueue, serverport);

		Thread.sleep(200);
		serverport= friendOverlay.getOrga().getServer_port();

		// Get Address of network interface which shall be used
		System.out.println("Please choose your communication Interface/Network.\n");
		Enumeration<NetworkInterface> enumNetworks = NetworkInterface.getNetworkInterfaces();
        int ctr = 1;
        ArrayList<String> ipList = new ArrayList<String>();
		for (;enumNetworks.hasMoreElements();) {
                NetworkInterface networkInt = enumNetworks.nextElement();
                System.out.println(ctr + " - Interface: " + networkInt.getName());
                Enumeration<InetAddress> addressList = networkInt.getInetAddresses();
                for (;addressList.hasMoreElements();) {
                		InetAddress addr = addressList.nextElement();
                        System.out.println("#Address: " + addr.getHostAddress());
                        ipList.add(addr.getHostAddress());
                }
                ctr++;
                System.out.println("");
        }
        
		boolean running = true;
		while(running){
			try {
	            input = scanner.nextInt();
	        }catch (java.util.InputMismatchException e){
	            scanner.next();
	        }
			
			if (input <= (ipList.size()-ipList.size()/2)){
				running = false;
			} else {
				System.out.println("Invalid Input. Please choose a number.");
			}
		}
    
    
	    String ip = "";
		int counter = 0;
		Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
		for (;networks.hasMoreElements() && counter < input;) {
		  counter++;
		  NetworkInterface networkInt = networks.nextElement();
		  if (input == counter) {
			  Enumeration<InetAddress> addr = networkInt.getInetAddresses();
			  ip = addr.nextElement().getHostAddress();
		      if (ip.contains(":") && addr.hasMoreElements()) {        
		    	  ip = addr.nextElement().getHostAddress();
		      }
		  }
		}

		//initialize connection overlay
    ConnectionInterface connectionOverlay = new ConnectionInterface(key, pub, connectionOverlayQueue, serverport, ip);
    connectionOverlay.setHome(home);
		//initialize Gui, comment out this line if Commandline interaction is preferred

		/*MainFrame m =new MainFrame(connectionOverlay, friendOverlay, pub, key, home, ip);
		m.interactionAndUpdate();*/

		//For reading input from the user
        running = true;
        while(running){
        	System.out.println(
        			"1 - Connect to Connection Overlay \n" +
        			"2 - Send friend request in Connection Overlay\n" +
        			"3 - Request file\n" +
					"4 - Add friend\n" +
        			"5 - Check Connection\n" +
					"6 - Exit \n");
        	input = 0;
        	try {
                input = scanner.nextInt();
            }catch (java.util.InputMismatchException e){
                scanner.next();
            }catch (java.util.NoSuchElementException e){
            	// shutdown
            }


            switch(input){
	        	case 1:	connectionOverlay.connectToNetwork();
	        			break;
	        			
	        	case 2: connectionOverlay.sendFriendRequest();
	        			break;
	        			
	        	case 3: friendOverlay.requestFile();
	        			break;

				case 4: friendOverlay.getFriendFromCommandline();
						break;
				
				case 5:	if(connectionOverlay.getActiveConnectionList().isEmpty()){
							System.out.println("There is no connection to the public network at the moment.\n");
						} else {
							System.out.println("You are connected to the public network.\n");
						}
						break;

				case 6: try {
							connectionOverlay.closeConnectionOverlay();
							friendOverlay.getOrga().goOffline();
		        			running = false;
		        			scanner.close();
						} catch (InterruptedException e) {
							System.out.println("Error shutting down connection overlay.");
							e.printStackTrace();
						}
	        			break;

	        	case 7: connectionOverlay.testSignal();
	        			break;
	        			
	        	default: System.out.println("Invalid input. Please select one of the following options.\n");
	        			 break;
        		
        	}
            
        }
        
        System.exit(0);
    }
}
