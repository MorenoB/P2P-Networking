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
        
    public SearchMessage(PeerReference sourcePeerReference, PeerReference targetPeerReference)
    {      
        super("PEER FOUND RESPONSE");
        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = targetPeerReference;
    }
    
    public SearchMessage(PeerReference sourcePeerReference, int peerId)
    {      
        super("PEER SEARCHING");
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
