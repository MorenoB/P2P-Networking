package communication.messages;

/**
 *
 */
public class SpeedMessage extends Message {

    public SpeedMessage(int messageType) {
        super(messageType);
    }

    @Override
    public String generateXml() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*private float speed;
    private String dateString;

    public SpeedMessage(float speed, String dateString) {
        super(Message.SPEED);
        this.speed = speed;
        this.dateString = dateString;
    }

    @Override
    public String generateXml() {
        String message = "";
        message += "<id>" + getId() + "</id>";
        message += "<SpeedMessage>";
        message += "<Speed>";
        message += speed;
        message += "</Speed>";
        message += "<DateString>";
        message += dateString;
        message += "</DateString>";
        message += "</SpeedMessage>";

        return message;
    }

    public float getSpeed() {
        return speed;
    }*/
}
