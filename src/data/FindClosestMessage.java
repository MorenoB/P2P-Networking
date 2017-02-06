package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moreno
 */
public class FindClosestMessage extends Message implements IMessage {

    private final PeerReference targetPeerReference;
    private final PeerReference sourcePeerReference;
    
    private boolean hasTargetReference = false;
    
    private int originalSearchId;

    public FindClosestMessage(boolean onlySearchForConnection, PeerReference sourcePeerReference, PeerReference targetPeerReference) {
        super(Constants.MSG_RESPONSE_SEARCH_PEERREF);
        this.msg = "RESPONSE PEER-REFERENCE";

        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = targetPeerReference;
    }

    public FindClosestMessage(boolean onlySearchForConnection, PeerReference sourcePeerReference, int peerId) {
        super(Constants.MSG_REQUEST_SEARCH_PEERREF);
        this.msg = "REQUEST PEER-REFERENCE";

        this.sourcePeerReference = sourcePeerReference;
        this.targetPeerReference = new PeerReference(peerId, "NULL", -1);
    }

    public void setOriginalSearchId(int originalSearchId) {
        this.originalSearchId = originalSearchId;
    }

    public int getOriginalSearchId() {
        return originalSearchId;
    }
    
    public void setHasTargetReference(boolean value)
    {
        hasTargetReference = value;
    }

    public PeerReference getTargetPeerReference() {
        return targetPeerReference;
    }

    public PeerReference getSourcePeerReference() {
        return sourcePeerReference;
    }

    public boolean getHasTargetReference() {
        return hasTargetReference;
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
