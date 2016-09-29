package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moreno
 */
public class PeerReference extends Message implements IMessage{
    
    private final int Id;
    private final String address;
    private final int portNumber;

    public PeerReference(int Id, String address, int portNumber) {
        super(Constants.MSG_JOIN);
        this.Id = Id;
        this.address = address;
        this.portNumber = portNumber;
    }

    public String getAddress() {
        return address;
    }

    public int getId() {
        return Id;
    }

    public int getPortNumber() {
        return portNumber;
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
