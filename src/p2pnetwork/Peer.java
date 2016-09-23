package p2pnetwork;

import Interfaces.ICommunicationListener;
import Util.Constants;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import java.util.logging.Level;
import java.util.logging.Logger;
import communication.messages.Message;

/**
 *
 * @author Moreno
 */
public class Peer implements ICommunicationListener {

    private static final Logger LOGGER = Logger.getLogger(Peer.class.getCanonicalName());

    public boolean isRunning;

    private Server server;
    private Client client;

    private boolean sentPeerRequest;
    private byte peerID;

    public Peer(byte peerID) {
        isRunning = true;
        
        //Peers that have peerid set to -1 are considered disconnected from the peer network.
        peerID = Constants.DISCONNECTED_PEERID;

        this.peerID = peerID;
    }

    public void Start() {
        server = new Server();

        client = new Client();

        server.AddListener(this);
        client.AddListener(this);

        Thread serverThread = new Thread(server);
        Thread clientThread = new Thread(client);

        //Start Threads
        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        clientThread.start();

        client.SetupConnection("localhost", Constants.SERVERPORT);

        while (!client.isReady() || !server.isReady()) {
            //
        }

        while (isRunning) {
            Update();
        }
    }

    private void SetID(byte newId) {
        peerID = newId;
    }

    private void Update() {
        
        if(!client.HasConnection())
            return;
        
        if(isDisconnectedFromNetwork() && !sentPeerRequest)
            RequestPeerId();
    }

    private void Stop() {
        client.Stop();
        server.Stop();

        isRunning = false;
    }
    
    private void JoinNetworkWithPeerId(byte peerId)
    {
        //When peers join a network, they are getting a peer id
        SetID(peerID);
        
        LOGGER.log(Level.INFO, "Peer id set to {0}", peerID);
    }
    
    private void DisconnectFromNetwork()
    {
        //When peers join a network, they are getting a peer id
        SetID(Constants.DISCONNECTED_PEERID);
        
        LOGGER.log(Level.INFO, "Peer is disconnected! Peer id set to {0}", Constants.DISCONNECTED_PEERID);
        sentPeerRequest = false;
    }
    
    private void RequestPeerId()
    {
        Message peerRequestMessage = MessageParser.CreatePeerIDRequest();
        client.writeMessage(peerRequestMessage);
        sentPeerRequest = true;
    }
    
    private boolean isDisconnectedFromNetwork()
    {
        return peerID == Constants.DISCONNECTED_PEERID;
    }

    @Override
    public void OnClientStarted() {
        LOGGER.log(Level.INFO, "Client has started!");
    }

    @Override
    public void OnClientConnectedToServer() {
        LOGGER.log(Level.INFO, "Client connected to a server!");
    }

    @Override
    public void OnClientDisconnected() {
        LOGGER.log(Level.INFO, "Client has disconnected!");
    }

    @Override
    public void OnClientError() {
        LOGGER.log(Level.SEVERE, "Client has an error!");
    }

    @Override
    public void OnClientSentMessage() {
        LOGGER.log(Level.INFO, "Client has sent a message!");
    }

    @Override
    public void OnClientRecievedMessage() {

        Message recievedMsg = client.getMessage();

        switch (recievedMsg.getMessageType()) {
            case Constants.MSG_MESSAGE:
                LOGGER.log(Level.INFO, "Client recieved message = {0}", recievedMsg.getMsg());
                break;
                
            case Constants.MSG_IPADDRESS:
                LOGGER.log(Level.INFO, "Client recieved ip adress = {0}", recievedMsg.getMsg());
                break;
                
            case Constants.MSG_PEERID:
                
                String recievedString = recievedMsg.getMsg();
                Byte recievedId = Byte.parseByte(recievedString);
                JoinNetworkWithPeerId(recievedId);
                        
                LOGGER.log(Level.INFO, "Client recieved peer id = {0}", recievedString);
                break;
                
            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Client recieved quit command!");
                Stop();
                break;
        }
    }

    @Override
    public void OnServerStarted() {
        LOGGER.log(Level.INFO, "Server started!");
    }

    @Override
    public void OnServerAcceptedConnection() {
        LOGGER.log(Level.INFO, "Server has accepted incoming connection!");
    }

    @Override
    public void OnServerError() {
        LOGGER.log(Level.SEVERE, "Server has an error!");
    }

    @Override
    public void OnServerSentMessage() {
        LOGGER.log(Level.INFO, "Server has sent a message!");
    }

    @Override
    public void OnServerRecievedMessage() {

        Message recievedMsg = server.getMessage();

        switch (recievedMsg.getMessageType()) {
            case Constants.MSG_MESSAGE:
                //LOGGER.log(Level.INFO, "Server recieved message = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_IPADDRESS:
                //LOGGER.log(Level.INFO, "Server recieved ip adress = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_PEERID:
                //LOGGER.log(Level.INFO, "Server recieved peer id = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Server recieved quit command!");
                Stop();
                
            case Constants.MSG_REQUEST_PEERID:
                server.writeMessage(MessageParser.CreatePeerIDMessage(peerID));
                
                break;
        }
    }

}
