package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moreno
 */
public class Search extends Message implements IMessage{

    private final PeerReference targetPeerReference;
    private final PeerReference sourcePeerReference;
        
    public Search(PeerReference sourcePeerReference, PeerReference targetPeerReference)
    {      
        super("PEER FOUND RESPONSE");
        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = targetPeerReference;
    }
    
    public Search(PeerReference sourcePeerReference, int peerId)
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
        return messageType == Constants.MSG_RESPONSE_SEARCH_FOR_ID;
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
