package communication;

/**
 * Interface for things that can process messages.
 */
public interface IProcessesMessage {
    public int getId();

    public void setOccupied(boolean occupied);

    public int getProcessingMessageId();
    public void setProcessingMessageId(int id);
}
