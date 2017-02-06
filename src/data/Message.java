package data;

import Interfaces.IMessage;
import Util.Constants;
import java.util.UUID;

public class Message implements IMessage {

    protected final int messageType;
    protected String guid;
    protected String msg;
    protected int targetId;

    public Message(int messageType) {
        this.messageType = messageType;
        this.guid = UUID.randomUUID().toString();
    }

    public Message(String msg) {
        this.messageType = Constants.MSG_MESSAGE;
        this.guid = UUID.randomUUID().toString();
        this.msg = msg;
    }

    @Override
    public String getGuid() {
        return guid;
    }
    
    public void setGuid(String newGuid)
    {
        this.guid = newGuid;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
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
