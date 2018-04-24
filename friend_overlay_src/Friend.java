package friend_overlay_src;

import javax.crypto.SecretKey;
import java.security.PublicKey;

/**
 * Friend is a class for saving information about contacts from the FriendOverlay.
 * It includes name, ip, port and publickey of the friend.
 * It also stores infomation about the sessionkey for the connection if it was already established.

 */
public class Friend {

	public String id;
	public String ip;
	public int port;
	public PublicKey publickey;
	public SecretKey skey;
    private Keys k;

	/**
	 * Constructor for Friend.
	 * If the port is not known, it can be set to 0.
	 * If the ip is not known, it can be set to 0.0.0.0.
	 * @param id
	 * @param ip
	 * @param port
	 * @param pubkey
	 */
	Friend(String id, String ip, int port, PublicKey pubkey){
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.publickey = pubkey;
		this.k = new Keys();
	}

	public String toString(){
		String pub = k.keytoString(publickey);
		return "("+this.id+","+this.ip+","+Integer.toString(this.port)+","+pub;
	}

	/**
	 * This is not a network-wide id but local.
	 * @return name or handle for friend
	 */
	public String getID() {
		return id;
	}

	public void setID(String name) {
		this.id = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public PublicKey getPublickey() {
		return publickey;
	}

	public void setPublickey(PublicKey publickey) {
		this.publickey = publickey;
	}

	/**
	 * Checks if two Friend objects are the same based on ip, port, publickey and id.
	 * @param f
	 * @return true if two Friend objects are the same in ip, port, publickey and id.
	 */
	public boolean equals(Friend f){
		if(this.ip!=f.getIp()){
			return false;
		}else if(this.port!=f.getPort()){
			return false;
		}else if(this.publickey!=f.getPublickey()){
			return false;
		}else if(this.id!=this.getID()){
			return false;
		}
		return true;

	}

	/**
	 * @return Sessionkey of Connection with Friend
	 */
	public SecretKey getSkey() {
		return skey;
	}

	public void setSkey(SecretKey skey) {
		this.skey = skey;
	}
}
