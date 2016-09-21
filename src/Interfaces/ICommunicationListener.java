package Interfaces;

/**
 *
 * @author Moreno
 */
public interface ICommunicationListener {
    public void OnClientStarted();
    public void OnClientConnectedToServer();
    public void OnClientDisconnected();
    public void OnClientError();
    
    public void OnClientSentMessage();
    public void OnClientRecievedMessage();
    
    public void OnServerStarted();
    public void OnServerAcceptedConnection();
    public void OnServerError();
    
    public void OnServerSentMessage();
    public void OnServerRecievedMessage();
}
