package Interfaces;

/**
 *
 * @author Moren
 */
public interface IPeerListener {
    public void OnMessageReceived(IMessage message);
    public void OnMessageSent();
}
