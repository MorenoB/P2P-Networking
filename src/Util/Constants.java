package Util;

/**
 *
 * @author Moreno
 */
public final class Constants {
    
    public static final int CYCLEWAIT = 200;
    
    public static final int SERVERPORT = 1200;
    
    public static final byte DISCONNECTED_PEERID = -1;
    
    public static final int P2PSIZE = 8;
    public static final byte PEERREFERENCE_SIZE = 2;
    
    public static final int MAX_CONNECTION_RETRIES = 4;
    
    public static final int BOOTPEER_ID = P2PSIZE;
    
    public static final boolean ALLOW_PERIODICALLY_CHECK_PEER_REFERENCES_ARE_ALIVE = false;
    public static final int PEER_WILL_PING_AFTER_SECONDS = 3;
    
    
    //Message types
    public static final int MSG_QUIT = 1;
    public static final int MSG_MESSAGE = 2;
    public static final int MSG_PEERID_RESPONSE = 3;
    public static final int MSG_REQUEST_PEERID = 4;
    
    public static final int MSG_REQUEST_SEARCH_PEERREF = 8;
    public static final int MSG_RESPONSE_SEARCH_PEERREF = 9;
    
    public static final int MSG_RESPONSE_ROUTINGTABLE = 11;
    
    public static final int MSG_REQUEST_CLOSEST = 12;
    public static final int MSG_RESPOSE_CLOSEST = 13;
    
    public static final int MSG_JOIN = 6;
    
    public enum PEER_STATUS { WAITINGFORCLOSESTRESPONSE, RESPONDING, SENDINGMSG, IDLE, JOINING, COPYINGROUTETABLE }
}
