package data;

import Interfaces.IMessage;
import java.util.UUID;

public class MessageObject implements IMessage {

    protected final int messageType;
    protected String guid;
    protected String msg;
    protected int targetId;

    public MessageObject(int messageType) {
        this.messageType = messageType;
        this.guid = UUID.randomUUID().toString();
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
