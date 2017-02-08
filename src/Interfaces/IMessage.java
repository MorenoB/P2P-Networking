package Interfaces;

/**
 *
 * @author Moreno
 */
public interface IMessage {
    
    public int getMessageType();
    public String getMsg();
    public String getGuid();
    public int getTargetId();
}
