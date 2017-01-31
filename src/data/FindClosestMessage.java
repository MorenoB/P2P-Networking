package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moren
 */
public class FindClosestMessage extends Message implements IMessage{
    
    private PeerReference sourcePeerReference;
    
    public FindClosestMessage(boolean isRequest) {
        super(isRequest ? Constants.MSG_REQUEST_CLOSEST : Constants.MSG_RESPOSE_CLOSEST);
    }
    
}
