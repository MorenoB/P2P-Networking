package communication.messages;

public class Message {

    public static final int QUIT = 1;
    public static final int MESSAGE = 2;

    private static int counter = 0;
    private final int messageType;
    private final int id;
    private String msg;
    
    public Message(int messageType, int id)
    {
        this.messageType = messageType;
        this.id = id;
    }

    public Message(int messageType) {
        this.messageType = messageType;
        counter += 1;
        this.id = counter;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getId() {
        return id;
    }
    
    public String getMsg()
    {
        return msg;
    }
    
    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}
