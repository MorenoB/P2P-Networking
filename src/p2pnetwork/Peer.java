package p2pnetwork;

import Interfaces.ICommunicationListener;
import Util.Constants;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import java.util.logging.Level;
import java.util.logging.Logger;
import communication.messages.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author Moreno
 */
public class Peer implements ICommunicationListener, Runnable {

    private static final Logger LOGGER = Logger.getLogger(Peer.class.getCanonicalName());

    public boolean isRunning;

    private Server server;
    private Client client;

    private boolean bootPeer;
    private byte peerID;

    private final List<Byte> availablePeerIds;
    private final LinkedHashMap<Byte, String> peerReferences;

    public Peer(byte peerID) {

        this.availablePeerIds = new ArrayList<>();
        isRunning = true;

        this.peerID = peerID;

        peerReferences = new LinkedHashMap<>(4);

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

        while (!server.isReady()) {
            //
        }

        PopulatePeerreferences();
    }

    public void ConnectToPeer(String ipAddress) {
        if (client.HasConnection()) {
            LOGGER.log(Level.WARNING, "Client already has a connection while setting up a new connection!");
            client.StopConnection();
        }
        client.SetupConnection(ipAddress, Constants.SERVERPORT);
    }

    private void SetID(byte newId) {
        peerID = newId;
    }

    private void PopulatePeerreferences() {
        byte otherId = peerID;

        for (int i = 0; i < Constants.INITIAL_HASHMAP_SIZE; i++) {
            otherId = (byte) Math.floorMod((int) (peerID + Math.pow(2, i)), Constants.P2PSIZE);
            peerReferences.put(otherId, "test" + otherId);
            //LOGGER.log(Level.INFO, getPeerreferenceByIndex(i).toString());
            LOGGER.log(Level.INFO, "Distance between my ({0}) and other ({1}) = {2}", new Object[]{peerID, getPeerreferenceByIndex(i).getKey(), CalculateDistance(peerID, (byte) getPeerreferenceByIndex(i).getKey())});
        }
    }

    private Entry getPeerreferenceByIndex(int index) {
        Iterator iterator = peerReferences.entrySet().iterator();
        int n = 0;
        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (n == index) {
                return entry;
            }
            n++;
        }
        return null;
    }

    private byte CalculateDistance(byte srcID, byte destID) {
        byte result = (byte) Math.floorMod(destID - srcID + Constants.P2PSIZE, Constants.P2PSIZE);

        return result;
    }

    private void Update() {

        /*if(client == null) return;

        if (!client.HasConnection()) {
            return;
        }*/
    }

    public void JoinNetworkWithIP(String ipAdress, int port) {
        client.SetupConnection(ipAdress, port);

        while (!client.isReady()) {
            //
        }

        RequestPeerId();
    }

    private Byte CalculatePeerId(Byte curId, Byte difId, Byte timesRecursed) {
        timesRecursed++;

        return curId;
    }

    private void Stop() {
        client.Stop();
        server.Stop();

        isRunning = false;
    }

    private void JoinNetworkWithPeerId(byte peerId) {
        //When peers join a network, they are getting a peer id
        SetID(peerId);

        LOGGER.log(Level.INFO, "Peer id set to {0}", peerId);
    }

    private void DisconnectFromNetwork() {
        //When peers join a network, they are getting a peer id
        SetID(Constants.DISCONNECTED_PEERID);

        LOGGER.log(Level.INFO, "Peer is disconnected! Peer id set to {0}", Constants.DISCONNECTED_PEERID);
    }

    private void RequestPeerId() {
        Message peerRequestMessage = MessageParser.CreatePeerIDRequest();
        client.writeMessage(peerRequestMessage);
        LOGGER.log(Level.INFO, "Requesting peer id to server...");
    }

    private boolean isDisconnectedFromNetwork() {
        return peerID == Constants.DISCONNECTED_PEERID;
    }

    public void setBootPeer(boolean active) {
        bootPeer = active;
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
                SetID(recievedId);

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

                for (int i = 0; i < availablePeerIds.size(); i++) {
                    byte assignedId = availablePeerIds.get(i);
                    server.writeMessage(MessageParser.CreatePeerIDMessage(assignedId));

                    availablePeerIds.remove(i);
                    break;
                }

                break;
        }
    }

    @Override
    public void run() {

        while (isRunning) {
            Update();
        }
    }

}
