package project_main_src;

import connection_overlay_src.ConnectionInterface;
import friend_overlay_src.*;

import javax.swing.*;
import java.awt.event.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Vector;

public class MainFrame {
    JFrame f;
    ConnectionInterface connectionOverlay;
    FriendOverlay friendOverlay;
    PublicKey publicKey;
    PrivateKey privateKey;
    List<Friend> contacts;

    JButton b;
    JButton b2;
    JButton b3;
    JButton b4;
    JButton b_ok;
    JButton b_ok2;

    JTextField c_port;
    JTextField c_ip;
    JTextArea tf_connected_friend_overlay;
    JTextField tf_connected_conn_overlay;

    JLabel label1;
    JLabel label2;
    JLabel label3;
    JLabel label4;
    JLabel label5;
    JLabel label6;
    JLabel label7;

    JTextField tf_port;
    JTextField tf_ip;
    JTextField tf_name;
    JTextField tf_pubkey;

    JComboBox list;
    JComboBox list2;

    boolean c_connected;
    int pcs;
    String username;

    MainFrame(ConnectionInterface connectionOverlay, FriendOverlay friendOverlay, PublicKey publicKey, PrivateKey privateKey,
              String username, String ip ) {
        this.connectionOverlay=connectionOverlay;
        this.friendOverlay=friendOverlay;
        this.privateKey=privateKey;
        this.publicKey=publicKey;
        Parser parser = new Parser();
        parser.setCSVLocation(username);
        this.contacts = parser.getFriendsFromFile();
        this.c_connected=false;
        this.pcs=0;
        this.username=username;

        // ----------------Hauptbuttons-----------------------------------------------------------------------------
        f = new JFrame();//creating instance of JFrame

        b = new JButton("Connect to Connection Overlay");
        b2 = new JButton("Send friend request in Connection Overlay");
        b3 = new JButton("request file");
        b4 = new JButton("Add a friend");

        String my_port=connectionOverlay.getListenerPort();
        c_port=new JTextField("Connection Overlay Port: "+my_port);
        c_port.setEditable(false);
        c_ip=new JTextField("IP: "+ip);
        c_ip.setEditable(false);
        tf_connected_conn_overlay=new JTextField("Not connected to Connection Overlay yet.");
        tf_connected_conn_overlay.setEditable(false);
        tf_connected_friend_overlay=new JTextArea("Not connected to any friends yet.");
        tf_connected_friend_overlay.setEditable(false);


        b.setBounds(130, 30, 500, 20);
        b2.setBounds(130, 60, 500, 20);
        b3.setBounds(130, 90, 500, 20);
        b4.setBounds(130, 120, 500, 20);

        c_port.setBounds(130,5, 300,20);
        c_ip.setBounds(450,5, 180,20);
        tf_connected_conn_overlay.setBounds(130,320, 500,20);
        tf_connected_friend_overlay.setBounds(130,350, 500,100);

        f.add(b);
        b2.setVisible(false);
        f.add(b2);
        b3.setVisible(false);
        f.add(b3);
        f.add(b4);

        f.add(c_port);
        f.add(c_ip);
        f.add(tf_connected_conn_overlay);
        f.add(tf_connected_friend_overlay);


        //------------------später sichtbare objecte---------------------------------------------------------
        label1 = new JLabel("Port for bootstrapping");
        label1.setBounds(130,150, 220,20);
        label2 = new JLabel("IP for bootstrapping");
        label2.setBounds(370,150, 220,20);
        label3 = new JLabel("Choose a file requested file.");
        label3.setBounds(130,150, 220,20);
        label4 = new JLabel("Port (if unknown: 0) ");
        label4.setBounds(130,150, 220,20);
        label5 = new JLabel("IP (if unknown: 0.0.0.0) ");
        label5.setBounds(370,150, 220,20);
        label6 = new JLabel("Name/Handle for Friend");
        label6.setBounds(130,190, 220,20);
        label7 = new JLabel("Publickey of Friend");
        label7.setBounds(370,190, 220,20);

        tf_port=new JTextField();
        tf_port.setBounds(130,170, 220,20);
        tf_ip=new JTextField();
        tf_ip.setBounds(370,170, 220,20);
        tf_name=new JTextField();
        tf_name.setBounds(130,210, 220,20);
        tf_pubkey=new JTextField();
        tf_pubkey.setBounds(370,210, 220,20);

        list=new JComboBox();
        list.setBounds(130,170, 500,20);
        list2=new JComboBox();
        list2.setBounds(130,170, 500,20);


        b_ok = new JButton("Ok");
        b_ok.setBounds(285,250, 150,20);
        b_ok2 = new JButton("Ok");
        b_ok2.setBounds(285,250, 150,20);

        hideall();

        f.add(label1);
        f.add(label2);
        f.add(label3);
        f.add(label4);
        f.add(label5);
        f.add(label6);
        f.add(label7);

        f.add(tf_port);
        f.add(tf_ip);
        f.add(tf_name);
        f.add(tf_pubkey);

        f.add(list);
        f.add(list2);

        f.add(b_ok);
        f.add(b_ok2);

        //----Button funktionen-------------------------------------------------------------------------------
        //mit netzwerk verbinden
        option1();
        //friendrequest senden
        option2();
        //nach files im netzwerk suchen
        option3();
        //neue freunde hinzufügen
        option4();
        //Schließt Overlay Verbindungen bevor das Fenster geschlossen wird
        option5();

        //------------Frame Eigenschaften--------------------------------------------------------------------
        f.setTitle("Anonymous Filesharing - "+ username);
        f.setSize(800, 600);
        f.setLayout(null);//using no layout managers
        f.setVisible(true);//making the frame visible


    }


    /**
     * Connect to Connection Overlay
     */
    public void option1(){
        b.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                hideall();

                b_ok.setVisible(true);
                label1.setVisible(true);
                label2.setVisible(true);
                tf_port.setVisible(true);
                tf_ip.setVisible(true);

                b_ok.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String ipStr=tf_ip.getText();
                        String p=tf_port.getText();
                        boolean ok=true;

                        String regex="(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
                        if(!ipStr.matches(regex)){
                            JOptionPane.showMessageDialog(f, username+":Ip Address is not valid.");
                            ok=false;
                        }


                        int port=0;
                        try {
                            port = Integer.parseInt(p);
                        }catch (java.lang.NumberFormatException e1 ){
                            JOptionPane.showMessageDialog(f, username+":Port is not valid.");
                            ok=false;

                        }

                        String listenerPort=connectionOverlay.getListenerPort();
                        String listenerIp=connectionOverlay.getListenerIp();
                        if((Integer.toString(port).equals(listenerPort)) && ( (ipStr.equals(listenerIp) || (ipStr.startsWith("127."))) )){
                            JOptionPane.showMessageDialog(f, username+":You can not connect to yourself.");
                            ok=false;
                        }

                        if (ok) {

                            connectionOverlay.connectToNetworkViaGUI(port,ipStr);
                            c_connected=true;

                            label1.setVisible(false);
                            label2.setVisible(false);
                            tf_port.setVisible(false);
                            tf_ip.setVisible(false);
                            b_ok.setVisible(false);
                            b.setVisible(false);
                            b2.setVisible(true);

                            JOptionPane.showMessageDialog(f, username+":Connected to Connection Overlay.");
                            tf_connected_conn_overlay.setText("Connected to Connection Overlay.");

                        }

                    }
                });

            }
        });

    }

    /**
     * Send Friend Request
     */
    public void option2(){
        b2.addActionListener(new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            hideall();
            tf_port.setVisible(false);
            tf_ip.setVisible(false);
            b_ok.setVisible(false);

            try {
                list.removeAllItems();
            }catch (ArrayIndexOutOfBoundsException e1){}

            for(int i=1; i<=contacts.size(); i++){
                Friend f=contacts.get(i-1);
                String name=f.getID();
                String ip=f.getIp();
                int port=f.getPort();

                String s= name+ " (IP "+ip+" ,Port "+port+")";
                list.addItem(s);
            }


            if(contacts.size()==0){

                list.addItem("No friends yet");
            }

            list.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    int index=list.getSelectedIndex();
                    String name=contacts.get(index).id;
                    System.out.println("Length of contacts "+contacts.size());

                    if(name!=null) {
                        Keys k = new Keys();
                        String keystring = k.keytoString(publicKey);
                        connectionOverlay.sendFriendRequestViaGui(index, contacts, keystring);
                        list.setVisible(false);
                        b3.setVisible(true);
                        list.removeAllItems();

                        JOptionPane.showMessageDialog(f, username+":Sent FriendRequest to Friend " + name + " via the Connection Overlay.");
                    }
                }
            });


            list.setVisible(true);
        }
    });
    }

    /**
     * Requesting Files via Gui
     */
    public void option3(){
        b3.addActionListener(new ActionListener () {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideall();
                try {
                    list2.removeAllItems();
                }catch (ArrayIndexOutOfBoundsException e1){}

                Vector<String> files = friendOverlay.getOrga().getFiles();

                if (files.size() > 0) {

                    for (String file : files) {
                         list2.addItem(file);
                    }


                    }else{
                    list2.addItem("No Files available.");
                    }
                    label3.setVisible(true);
                    list2.setVisible(true);

                list.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String s= (String) list.getSelectedItem();
                        friendOverlay.getOrga().request(s);

                        list2.setVisible(false);
                        label3.setVisible(false);
                        tf_port.setVisible(false);
                        list2.removeAllItems();
                        list2.removeAll();

                        JOptionPane.showMessageDialog(f, username+": Sent Request for "+s);

                    }
                });
            }
        });

    }

    /**
     * Add new friend to system.
     */
    public void option4(){

        b4.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                hideall();
                b_ok2.setVisible(true);
                label4.setVisible(true);
                label5.setVisible(true);
                label6.setVisible(true);
                label7.setVisible(true);

                tf_port.setVisible(true);
                tf_ip.setVisible(true);
                tf_name.setVisible(true);
                tf_pubkey.setVisible(true);


                b_ok2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String ipStr = tf_ip.getText();
                        String p = tf_port.getText();
                        String name=tf_name.getText();
                        String friend_pubkey=tf_pubkey.getText();

                        boolean ok = true;

                        String regex = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
                        if (!ipStr.matches(regex)) {
                            JOptionPane.showMessageDialog(f, username+": Ip Address is not valid.");
                            ok = false;
                        }


                        int port = 0;
                        try {
                            port = Integer.parseInt(p);
                        } catch (java.lang.NumberFormatException e1) {
                            JOptionPane.showMessageDialog(f, username+": Port is not valid.");
                            ok = false;

                        }

                        if(name==""){
                            JOptionPane.showMessageDialog(f, username+":Name can not be the empty string.");
                            ok=false;
                        }

                        if(friend_pubkey.length()!= 128){
                            JOptionPane.showMessageDialog(f, username+":Public key of friend is not valid. It has to be 128 characters long.");
                            ok=false;
                        }

                        if(ok){
                            friendOverlay.saveNewFriend(name,ipStr, port,friend_pubkey);
                            JOptionPane.showMessageDialog(f, username+":Added Friend "+name);

                            tf_ip.setVisible(false);
                            tf_name.setVisible(false);
                            tf_pubkey.setVisible(false);
                            tf_port.setVisible(false);
                            label4.setVisible(false);
                            label5.setVisible(false);
                            label6.setVisible(false);
                            label7.setVisible(false);
                            b_ok2.setVisible(false);

                        }

                    }
                });


            }
        });



    }


    /**
     * Shutting down overlays when Gui is closed
     */
    public void option5(){
        WindowListener exitListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    connectionOverlay.closeConnectionOverlay();
                    friendOverlay.getOrga().goOffline();
                    System.exit(0);
                } catch (InterruptedException e1) {
                    System.out.println("Error shutting down connection overlay.");
                    e1.printStackTrace();
                    System.exit(1);
                }
            }
        };
        f.addWindowListener(exitListener);

    }

    /**
     * Hide all unnecessary elements in GUI.
     */
    public void hideall(){
        label1.setVisible(false);
        label2.setVisible(false);
        label3.setVisible(false);
        label4.setVisible(false);
        label5.setVisible(false);
        label6.setVisible(false);
        label7.setVisible(false);

        tf_port.setVisible(false);
        tf_ip.setVisible(false);
        tf_name.setVisible(false);
        tf_pubkey.setVisible(false);

        b_ok.setVisible(false);
        b_ok2.setVisible(false);

        list.setVisible(false);
        list2.setVisible(false);

    }

    /**
     * Enables Updating GUI
     */
    public void interactionAndUpdate(){

        while(true){

            System.out.println("c_connected: "+c_connected+
                                " con_overlay: "+connectionOverlay.getActiveConnectionList().size()+
                                " fri_overlay: "+friendOverlay.getOrga().getPcs().size()

            );
            if(connectionOverlay.getActiveConnectionList().size()>0 && c_connected==false){

                JOptionPane.showMessageDialog(f, username+": You are now connected to the Connection Overlay. ");
                b.setVisible(false);
                b2.setVisible(true);
                c_connected=true;
                tf_connected_conn_overlay.setText("Connected to Connection Overlay.");

            }
            if(connectionOverlay.getActiveConnectionList().size()==0 && c_connected==true){
                b.setVisible(true);
                //JOptionPane.showMessageDialog(f, username+": You disconnected from Connection Overlay. ");
                tf_connected_conn_overlay.setText("Not connected to Connection Overlay anymore.");
                c_connected=false;

            }

            int open_pcs=friendOverlay.getOrga().getPcs().size();
            if(open_pcs>pcs){

                b3.setVisible(true);
                String new_text="";

                List<PeerConnection> pcs=friendOverlay.getOrga().getPcs();
                for(int i=0; i<pcs.size(); i++){
                    PeerConnection pc = pcs.get(i);
                    Friend f= pc.getFriend();
                    new_text=new_text+f.getID()+" (IP: "+f.getIp()+", Port: "+f.getPort()+")\n";
                }
                tf_connected_friend_overlay.setText(new_text);


            }


        }

    }


}