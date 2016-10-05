package Util;

/**
 *
 * @author Moreno
 */
public final class Constants {
    
    public static final int CYCLEWAIT = 100;
    
    public static final int SERVERPORT = 1222;
    
    public static final byte DISCONNECTED_PEERID = -1;
    
    public static final int P2PSIZE = 16;
    public static final byte INITIAL_HASHMAP_SIZE = 4;
    
    public static final int MAX_CONNECTION_RETRIES = 4;
    
    public static final int BOOTPEER_ID = 0;
    
    
    //Message types
    public static final int MSG_QUIT = 1;
    public static final int MSG_MESSAGE = 2;
    public static final int MSG_PEERID = 3;
    public static final int MSG_REQUEST_PEERID = 4;
    
    public static final int MSG_REQUEST_CONNECTIONINFO = 5;
    public static final int MSG_RESPONSE_CONNECTIONINFO = 7;
    
    public static final int MSG_REQUEST_SEARCH_PEERREF = 8;
    public static final int MSG_RESPONSE_SEARCH_PEERREF = 9;
    
    public static final int MSG_JOIN = 6;
}
