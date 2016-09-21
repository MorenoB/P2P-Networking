package communication.messages;

public abstract class Message {

    public static final int CREATE = 1;
    public static final int ARRIVE = 2;
    public static final int SPEED = 3;
    public static final int CRANE = 4;
    public static final int MOVE = 5;
    public static final int DEPART = 6;

    private static int counter = 0;
    private final int messageType;
    private int id;

    public Message(int messageType) {
        this.messageType = messageType;
        counter += 1;
        this.id = counter;
    }

    public abstract String generateXml();

    public int getMessageType() {
        return messageType;
    }

    public int getId() {
        return id;
    }
}
