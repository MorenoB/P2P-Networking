package p2pnetwork;

import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
import Util.Constants;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import java.util.logging.Level;
import java.util.logging.Logger;
import data.Message;
import data.PeerReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public class Peer implements ICommunicationListener, Runnable {

    private static final Logger LOGGER = Logger.getLogger(Peer.class.getCanonicalName());

    public boolean isRunning;

    private Server server;
    private Client client;

    private boolean clientIsConnected;
    private boolean bootPeer;
    private int peerID;
    private final int port;
    
    private int connectionRetries;

    private final List<Byte> availablePeerIds;
    private final List<PeerReference> peerReferences;

    private final ConcurrentLinkedQueue<Message> clientMessageQueue;

    public Peer(byte peerID, int port) {

        this.clientMessageQueue = new ConcurrentLinkedQueue<>();

        this.availablePeerIds = new ArrayList<>();
        this.isRunning = true;

        this.peerID = peerID;
        this.port = port;

        this.peerReferences = new ArrayList<>();

        for (int i = 0; i < Constants.INITIAL_HASHMAP_SIZE; i++) {
            peerReferences.add(null);
        }

    }

    public void Start() {

        if (bootPeer) {
            for (byte i = 0; i < Constants.P2PSIZE; i++) {
                if (i == this.peerID) {
                    continue;
                }
                availablePeerIds.add(i);
            }
        }

        server = new Server(port);

        client = new Client();

        server.AddListener(this);
        client.AddListener(this);

        Thread serverThread = new Thread(server, "ServerThead");
        Thread clientThread = new Thread(client, "ClientThread");

        //Start Threads
        serverThread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        clientThread.start();
    }

    public void ConnectToPeer(String ipAddress, int port) {
        if (client.HasConnection()) {
            LOGGER.log(Level.WARNING, "Client already has a connection while setting up a new connection!");
            DisconnectPeerFromOtherPeer();
        }
        client.SetupConnection(ipAddress, port);
    }

    private void DisconnectPeerFromOtherPeer() {
        //Notify server we want to close connection.
        clientMessageQueue.add(MessageParser.CreateQuitMessage());
    }

    private void SetID(byte newId) {
        peerID = newId;
    }

    private boolean AddPeerReference(PeerReference newPR) {

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference curRef = peerReferences.get(i);

            if (curRef != null) {
                continue;
            }

            peerReferences.set(i, newPR);

            return true;
        }

        return false;
    }

    public void ConnectToId(int id) {
        if (HasPeerReferenceId(id)) {
            ConnectToId(id);
            return;
        }

        PeerReference nextPR = FindShortestAvailablePeerRef();
        ConnectToPeerId(nextPR.getId());

        clientMessageQueue.add(new Message("Yoo peer " + nextPR.getId() + ", im looking for id " + id));
    }

    /**
     * Will try to connect to a peer if it has been found in the peerreference
     * list of this peer.
     *
     * @param id id to connect to
     * @return True if it was able to connect right away, false if the
     * peerreference was not found.
     */
    private boolean ConnectToPeerId(int id) {
        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef.getId() == id) {
                ConnectToPeer(peerRef.getAddress(), peerRef.getPortNumber());

                clientMessageQueue.add(new Message("HELLO FROM " + peerID));
                return true;
            }

        }

        return false;
    }

    private boolean HasPeerReferenceId(int id) {
        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef == null) {
                continue;
            }

            if (peerRef.getId() == id) {
                return true;
            }
        }

        return false;
    }

    private PeerReference FindShortestAvailablePeerRef() {
        int shortestDistanceIndex = -1;
        int shortestDist = Constants.P2PSIZE + 1;

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef == null) {
                continue;
            }

            int dist = CalculateDistance(peerID, peerRef.getId());

            if (dist < shortestDist) {
                shortestDist = dist;
                shortestDistanceIndex = i;
            }
        }

        return shortestDistanceIndex == -1 ? null : peerReferences.get(shortestDistanceIndex);

    }

    private int CalculateDistance(int srcID, int destID) {
        int result = Math.floorMod(destID - srcID + Constants.P2PSIZE, Constants.P2PSIZE);

        return result;
    }

    private void Update() {

        if (clientIsConnected && client != null) {
            Message msgToSend = clientMessageQueue.poll();

            if (msgToSend != null) {
                client.writeMessage(msgToSend);

                //Special case; Close down client if we detect a QUIT message.
                if (msgToSend.getMessageType() == Constants.MSG_QUIT) {
                    client.StopConnection();
                }
            }
        }
    }

    public void JoinNetworkWithIP(String ipAdress, int port) {
        
        if(!client.SetupConnection(ipAdress, port) && connectionRetries < Constants.MAX_CONNECTION_RETRIES)
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            connectionRetries++;
            JoinNetworkWithIP(ipAdress, port);
            return;
        }

        if (peerID == Constants.DISCONNECTED_PEERID) {
            RequestPeerId();
        }

        Message JoinPeerMsg = MessageParser.CreateJoinPeerMessage(peerID, getAddress(), getPort());

        clientMessageQueue.add(JoinPeerMsg);
    }

    private void Stop() {
        client.Stop();
        server.Stop();

        isRunning = false;
    }

    private void DisconnectFromNetwork() {
        //When peers join a network, they are getting a peer id
        SetID(Constants.DISCONNECTED_PEERID);

        LOGGER.log(Level.INFO, "Peer is disconnected! Peer id set to {0}", Constants.DISCONNECTED_PEERID);
    }

    private void RequestPeerId() {
        Message peerRequestMessage = MessageParser.CreatePeerIDRequest();
        clientMessageQueue.add(peerRequestMessage);
        LOGGER.log(Level.INFO, "Requesting peer id to server...");
    }

    private boolean isDisconnectedFromNetwork() {
        return peerID == Constants.DISCONNECTED_PEERID;
    }

    public void setBootPeer(boolean active) {
        bootPeer = active;
    }

    public int getId() {
        return (int) peerID;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return server.getAddress();
    }
    
    public String getPeerReferences()
    {
        String readablePeerReferences = "";
        for (int i = 0; i < peerReferences.size(); i++) {
             PeerReference peerRef = peerReferences.get(i);
             
            
            readablePeerReferences += peerRef == null ? " [NULL] " : " [" + peerRef.getId() + "] ";
        }
        
        return readablePeerReferences;
    }

    @Override
    public void OnClientStarted() {
        LOGGER.log(Level.INFO, "Client has started!");
    }

    @Override
    public void OnClientConnectedToServer() {
        clientIsConnected = true;

        LOGGER.log(Level.INFO, "Client connected to a server!");
    }

    @Override
    public void OnClientDisconnected() {
        clientIsConnected = false;

        LOGGER.log(Level.INFO, "Client has disconnected!");
    }

    @Override
    public void OnClientError() {
        LOGGER.log(Level.SEVERE, "Client has an error!");
    }

    @Override
    public void OnClientSentMessage(JSONObject jsonObj) {
        LOGGER.log(Level.INFO, "Client has sent {0}", jsonObj);
    }

    @Override
    public void OnClientRecievedMessage() {

        IMessage recievedMsg = client.getMessage();

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
                SetID(recievedId);

                //DisconnectPeerFromOtherPeer();

                LOGGER.log(Level.INFO, "Client recieved peer id = {0}", recievedString);
                break;

            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Client recieved quit command!");
                Stop();
                break;

            case Constants.MSG_JOIN:

                //After succesfully recieved an ACK from joining -> Disconnect
                DisconnectPeerFromOtherPeer();

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
        
        if(server != null)
            server.StopConnection();
    }

    @Override
    public void OnServerSentMessage(JSONObject jsonObj) {
        LOGGER.log(Level.INFO, "Server has sent {0}", jsonObj);
    }

    @Override
    public void OnServerRecievedMessage() {

        IMessage recievedMsg = server.getMessage();

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
                server.StopConnection();

            case Constants.MSG_REQUEST_PEERID:

                for (int i = 0; i < availablePeerIds.size(); i++) {
                    int assignedId = availablePeerIds.get(i);
                    server.writeMessage(MessageParser.CreatePeerIDMessage(assignedId));

                    availablePeerIds.remove(i);
                    break;
                }

                break;

            case Constants.MSG_JOIN:

                PeerReference newRef = (PeerReference) recievedMsg;

                if (AddPeerReference(newRef)) {
                    LOGGER.log(Level.INFO, "Added peer reference {0}", newRef);

                    server.writeMessage(recievedMsg);
                    break;
                }

                LOGGER.log(Level.INFO, "Unable to add peer reference {0}", newRef);
                //Failed to add reference to this peer, go to next peer!
                break;
        }
    }

    @Override
    public void run() {

        while (isRunning) {
                            
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            
            Update();
        }
    }
}
