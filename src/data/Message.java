package data;

import Interfaces.IMessage;
import Util.Constants;

public class Message implements IMessage {

    protected static int counter = 0;
    protected final int messageType;
    protected final int id;
    protected String msg;
    protected int targetId;

    public Message(int messageType, int id) {
        this.messageType = messageType;
        this.id = id;
    }

    public Message(int messageType) {
        this.messageType = messageType;
        counter += 1;
        this.id = counter;
    }

    public Message(String msg) {
        this.messageType = Constants.MSG_MESSAGE;
        counter += 1;
        this.id = counter;

        this.msg = msg;
    }

    public int getId() {
        return id;
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
