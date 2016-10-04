package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moreno
 */
public class SearchMessage extends Message implements IMessage{

    private final PeerReference targetPeerReference;
    private final PeerReference sourcePeerReference;
        
    public SearchMessage(boolean onlySearchForConnection, PeerReference sourcePeerReference, PeerReference targetPeerReference)
    {      
        super(onlySearchForConnection ? Constants.MSG_RESPONSE_CONNECTIONINFO : Constants.MSG_RESPONSE_SEARCH_PEERREF);
        this.msg = "RESPONSE PEER-REFERENCE";
        
        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = targetPeerReference;
    }
    
    public SearchMessage(boolean onlySearchForConnection, PeerReference sourcePeerReference, int peerId)
    {      
        super(onlySearchForConnection ? Constants.MSG_REQUEST_CONNECTIONINFO : Constants.MSG_RESPONSE_SEARCH_PEERREF);
        this.msg = "REQUEST PEER-REFERENCE";
        
        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = new PeerReference(peerId, "NULL", -1);
    }
    
    public PeerReference getTargetPeerReference()
    {
        return targetPeerReference;
    }
        
    public PeerReference getSourcePeerReference()
    {
        return sourcePeerReference;
    }
    
    public boolean getHasTargetPeerReference()
    {
        return messageType == Constants.MSG_RESPONSE_CONNECTIONINFO || messageType == Constants.MSG_RESPONSE_SEARCH_PEERREF;
    }

    @Override
    public int getMessageType() {
        return messageType;
    }

    @Override
    public String getMsg() {
        return msg;
    }
    
}
