package friend_overlay_src;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * This class enables encryption and  decryption with symmetric and asymmetric keys.
 * It also can read keys from file or create new key files.
 */
public class Keys {

    /**
     * Location of file for public key.
     */
    private String file_pub = "project_main_src/rsa.pub";

    /**
     * Location of file for private key.
     */
    private String file_priv = "project_main_src/rsa.key";

    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    /**
     * This method is called at startup of the programm to get a set of asymmetric keys.
     * It first tries to read keys from files by calling getPrivateKeyFromFile() and getPublicKeyfromFile().
     * If successful, it checks if those keys belong to the same pair.
     * Otherwise it will create a new pair of keys by calling createKeys().
     */
    public void initializeKeys() {


        getPrivateKeyFromFile();
        getPublicKeyfromFile();

        if (privateKey != null && publicKey != null) {

            if (isValidRSAPair(privateKey, publicKey)) {
                System.out.println("Keys: Read keys from file.");
            } else {
                System.err.println("Keys: Error when reading keys from file. Private key does not match public key.");
                privateKey = null;
                publicKey = null;

            }

        } else {
            createKeys();
            System.out.println("Keys: Creating new key pair.");

            if (privateKey == null && publicKey == null) {
                System.err.println("Keys: Initializing encryption failed. Program exits.");
                System.exit(1);

            }
        }

    }


    /**
     * Verifies if PrivateKey and Publickey belong to the same pair.
     * @param key
     * @param pubKey
     * @return true if keys belong to the same pair
     */
    public boolean isValidRSAPair(PrivateKey key, PublicKey pubKey) {
        if (key instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey pvt = (RSAPrivateCrtKey) key;
            BigInteger e = pvt.getPublicExponent();
            RSAPublicKey pub = (RSAPublicKey) pubKey;
            return e.equals(pub.getPublicExponent()) && pvt.getModulus().equals(pub.getModulus());
        } else {
            throw new IllegalArgumentException("Keys: Not a CRT RSA key.");
        }
    }


    /**
     * Creates a neew pair of asymmetric keys using the RSA algorithm.
     * Randomization is added to the key generation by using SecureRandom.
     * Privatekey should be of type  PKCS#8. Publickey should be of type X.509.
     * Keys are 512 characters long.
     * The keys are then saved to files by calling toFile() using the parameters file_pub and file_priv.
     */
    private void createKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");


            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            keyGen.initialize(512, random);
            KeyPair kp = keyGen.generateKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();


            toFile(publicKey, file_pub);
            toFile(privateKey, file_priv);

            // System.out.println("Private key format: " + pvt.getFormat());
            // prints "Private key format: PKCS#8" on my machine

            //System.out.println("Public key format: " + pub.getFormat());
            // prints "Public key format: X.509" on my machine

        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
        } catch (NoSuchProviderException e) {
            //e.printStackTrace();
        }
    }


    /**
     * This method creates session keys for the communication with a peer.
     * The session key is created by peer which receives the connection ('server').
     * @return SessionKey for connection to peer
     */
    public SecretKey generateSymmetricKey() {


        // Generate a AES key
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
            SecretKey key = keyGen.generateKey();
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;

    }


    /**
     * Saves a key (any type of key) to a file using the path 'filename'.
     * Before saving the key, it encodes it with Base64 to make it human-readable.
     * @param k
     * @param filename
     */
    private void toFile(Key k, String filename) {

        Base64.Encoder encoder = Base64.getEncoder();
        encoder.encodeToString(k.getEncoded());
        FileWriter out = null;
        try {
            out = new FileWriter(filename);
            out.write(encoder.encodeToString(k.getEncoded()));
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Reads PrivateKey from file in location file_priv.
     * PrivateKey has to be encoded with Base64 and of type PKCS8.
     */
    private void getPrivateKeyFromFile() {

        try {
            Path path = Paths.get(file_priv);

            byte[] bytes = Files.readAllBytes(path);
            byte[] bytes_decoded = Base64.getDecoder().decode(bytes);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes_decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(keySpec);

        } catch (InvalidKeySpecException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
            //passiert wenn es fehler im File gibt
        }
    }

    /**
     * Reads PublicKey from file at location file_pub.
     * Publickey has to be Base64 encoded and of type X509.
     */
    public void getPublicKeyfromFile() {
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");

            Path path = Paths.get(file_pub);
            byte[] bytes = Files.readAllBytes(path);
            byte[] bytes_decoded = Base64.getDecoder().decode(bytes);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes_decoded);
            publicKey = kf.generatePublic(keySpec);

        } catch (InvalidKeySpecException e) {
            // e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
            //passiert wenn es fehler im File gibt
        }

    }

    /**
     * Translates a Key (any Key) to a Base64 encoded String.
     * @param key
     * @return String of Key key
     */
    public String keytoString(Key key) {
        if (key == null) {

            new Exception().printStackTrace(System.err);
            System.err.println("Keys [keytoString]: Key was null.");
            return "";
        } else {
            Base64.Encoder encoder = Base64.getEncoder();
            String key_string = encoder.encodeToString(key.getEncoded());
            return key_string;
        }
    }

    /**
     * Translates a String s into a PublicKey object.
     * @param s
     * @return Public key for String s
     */
    public PublicKey StringToPublicKey(String s) {

        //System.out.println(s);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            byte[] bytes = s.getBytes();
            byte[] bytes_decoded = Base64.getDecoder().decode(s);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes_decoded);
            PublicKey pub = kf.generatePublic(keySpec);
            return pub;

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Translates String s into a session key of class SecretKey.
     * @param s
     * @return Secretkey from string
     */
    public SecretKey stringToSecretKey(String s) {

        byte[] bytes_decoded = Base64.getDecoder().decode(s);
        SecretKey skey = new SecretKeySpec(bytes_decoded, 0, bytes_decoded.length, "AES");
        return skey;


    }
    /**
     * Decrypts a String s unsing a provided PrivateKey key.
     * @param s
     * @param key
     * @return decrypted String  or empty string if decryption fails
     */
    public String decrypt(String s, PrivateKey key) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            Base64.Decoder decoder = Base64.getDecoder();
            String decrypted = new String(cipher.doFinal(decoder.decode(s)));
            return decrypted;

        } catch (NoSuchAlgorithmException 
        		| InvalidKeyException 
        		| NoSuchPaddingException
        		| IllegalBlockSizeException e) {
        	System.err.println("Keys: Decryption failed.Returning empty string.");
        	e.printStackTrace();
        	
        } catch (BadPaddingException e) {
            // System.err.println("Keys: Please check if public key for encryption matches private key. ");
            // e.printStackTrace();
        } catch(java.lang.IllegalArgumentException e){
        	e.printStackTrace();
        	System.out.println("Error decoding: "+s);
        }
        return "";
        
    }

    /**
     * Encrypts String s using provided Publickey pub.
     * @param s
     * @param pub
     * @return encrypted String or empty string if encryption fails
     */
    public String encrypt(String s, PublicKey pub) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            byte[] encrypted = cipher.doFinal(s.getBytes());
            Base64.Encoder encoder = Base64.getEncoder();
            String encryptedString = encoder.encodeToString(encrypted);
            return encryptedString;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        System.out.println("Keys: Encryption failed. Returning empty string.");
        return "";
    }

    /**
     * Encrypts String s in session key skey.
     * @param s
     * @param skey
     * @return encrypted String
     */
    public String encryptWithSkey(String s, SecretKey skey) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            byte[] encrypted = cipher.doFinal(s.getBytes());
            Base64.Encoder encoder = Base64.getEncoder();
            String encryptedString = encoder.encodeToString(encrypted);
            return encryptedString;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        System.out.println("Keys: Encryption failed. Returning empty string.");
        return "";
    }

    /**
     * Decrypts String s with session key skey.
     * @param s
     * @param skey
     * @return decrypted String
     */
    public String decryptWithSkey(String s, SecretKey skey) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skey);
            Base64.Decoder decoder = Base64.getDecoder();
            String decrypted = new String(cipher.doFinal(decoder.decode(s)));
            return decrypted;


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.err.println("Keys: Please check if public key for encryption matches private key. ");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        System.err.println("Keys: Decryption failed. Returning empty string.");
        return "";
    }

    /**
     * This method changes file_pub, the file path for the public key.
     * This is important, if a home directory is set in the MainClass.
     * @param name
     */
    public void setFilePub(String name) {
        this.file_pub = name;
    }

    /**
     * This method changes file_priv,  the file path for the private key.
     * This is important, if a home directory is set in the MainClass.
     * @param name
     */
    public void setFilePriv(String name) {
        this.file_priv = name;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}
