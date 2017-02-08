package data;

import Interfaces.IMessage;
import Util.Constants;
import java.util.List;

/**
 * Used for peers to communicate with the bootpeer; requesting a copy of the routing table.
 * Used for the bootpeer as well to respond with a copy of its routing table.
 * @author Moreno
 */
public class RoutingTableMessage extends MessageObject implements IMessage{
    
    private final List<PeerReference> routingTableCopy;
    private final int sourceId;
    
    public RoutingTableMessage(List<PeerReference> routingTableCopy)
    {
        super(Constants.MSG_RESPONSE_ROUTINGTABLE);

        this.msg = "ROUTING TABLE RESPONSE";
        
        this.sourceId = Constants.BOOTPEER_ID;
        this.routingTableCopy = routingTableCopy;
    }

    public List<PeerReference> getRoutingTableCopy() {
        return routingTableCopy;
    }

    public int getSourceId() {
        return sourceId;
    }
    
}
