package Interfaces;

import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public interface ICommunicationListener {
    public void OnClientStarted();
    public void OnClientConnectedToServer();
    public void OnClientDisconnected();
    public void OnClientError();
    
    public void OnClientSentMessage(JSONObject jsonObj);
    
    public void OnServerStarted();
    public void OnServerAcceptedConnection();
    public void OnServerError(int portNr);
    
    public void OnServerRecievedMessage();
}
