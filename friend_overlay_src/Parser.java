package friend_overlay_src;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class enables reading and writing friend objects from and to a file.
 */
public class Parser {
    private List<Friend> friends=null;

    /**
     * Location of friends.csv file.
     */
    private String csv_loc = "project_main_src/.friends.csv";


    /**
     * Reads all friends from friends.csv which is stored under csv_loc.
     * Creates a new friends.csv file if no file was found at csv_loc.
     * @return List of friend objects
     */
    public List<Friend> getFriendsFromFile() {
        BufferedReader bufferedReader = null;
        String line = "";
        String split = ",";
        this.friends = new ArrayList<Friend>();
        try {
            String header="id,ipaddress,port,publickey";
            bufferedReader = new BufferedReader(new FileReader(csv_loc));

            while ((line = bufferedReader.readLine()) != null) {

                //sorgt dafür, dass headerzeile ignoriert wird
                if (!line.contentEquals(header)) {

                    String[] infos = line.split(split);
                    Keys keys=new Keys();
                    Friend new_friend= new Friend(
                            infos[0],
                            infos[1],
                            Integer.parseInt(infos[2]),
                            keys.StringToPublicKey(infos[3]));

                    this.friends.add(new_friend);
                }

            }

        } catch (FileNotFoundException e) {
            //create file if no file was found
            createFriendsFile();
            System.out.println("Creating new .friends.csv file");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.friends;

    }


    /**
     * Creates new friends.csv file at location csv_loc.
     * After creation the file only includes a header.
     */
    private void createFriendsFile(){
        String header="id,ipaddress,port,publickey";
        
        File file = new File(csv_loc);


        try {
            file.createNewFile();
            FileWriter fw=new FileWriter(file);
            fw.write(header);
            fw.flush();
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Appends new friend object to friends.csv.
     * @param id
     * @param ip
     * @param port
     * @param publickey
     */
    public void addFriendtoCSV(String id, String ip, int port, String publickey){

        try {
            FileWriter pw = new FileWriter(csv_loc,true);
            pw.append("\n"+id+","+ip+","+port+","+publickey);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Was not able to write Information of Friend to friend.csv. Please alter the file manually and restart.");
        }

    }

    /**
     * Creates a String from all friend objects in list f.
     * @param f
     * @return String
     */
    public String toString(List<Friend> f){
        String s="";
        for (int i=0; i<f.size(); i++){
            s.concat(f.get(i).toString()+"\n");
        }
        return s;
    }

    public List<Friend> getFriends() {
        return friends;
    }


    /**
     * Changes location of friends.csv file to the subfolder home in src/project_main_src
     * @param home
     */
    public void setCSVLocation(String home ){
        this.csv_loc="project_main_src/"+home+"/.friends.csv";
    }

}
