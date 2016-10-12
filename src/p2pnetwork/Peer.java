package p2pnetwork;

import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
import Util.Constants;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import data.JoinMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import data.Message;
import data.PeerReference;
import data.SearchMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
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

    private int tryingToConnectToOtherId = -1;
    private int connectedToOtherId = -1;

    private final List<Byte> availablePeerIds;
    private final List<PeerReference> peerReferences;
    
    private PeerReference lastPeerRequest;

    private final List<String> processedGuids;

    private final ConcurrentLinkedDeque<Message> clientMessageQueue;

    public Peer(byte peerID, int port) {

        this.clientMessageQueue = new ConcurrentLinkedDeque<>();

        this.availablePeerIds = new ArrayList<>();
        this.isRunning = true;

        this.peerID = peerID;
        this.port = port;

        this.peerReferences = new ArrayList<>();

        this.processedGuids = new ArrayList<>();
        
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
        } else {
            AddBootPeer();
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

    private void ConnectToAddress(String ipAddress, int port) {
        if (client.HasConnection()) {
            LOGGER.log(Level.WARNING, "Client already has a connection while setting up a new connection!");
            DisconnectPeerFromOtherPeer(true);
        }
        client.SetupConnection(ipAddress, port);
    }

    private void DisconnectPeerFromOtherPeer(boolean instant) {
        //Notify server we want to close connection.
        
        Message quitMesssage = MessageParser.CreateQuitMessage(connectedToOtherId, port);
        
        if (instant) {
            client.writeMessage(quitMesssage);
            return;
        }
        if (clientMessageQueue.peek() == null) {
            clientMessageQueue.add(quitMesssage);
            return;
        }

        if (clientMessageQueue.peek().getMessageType() != Constants.MSG_QUIT) {
            clientMessageQueue.add(quitMesssage);
        }
    }

    private void SetID(byte newId) {
        LOGGER.log(Level.INFO, "Set peerId to {0}", newId);
        peerID = newId;

        if (peerID != Constants.DISCONNECTED_PEERID) {
            RequestPeerReferences(Constants.BOOTPEER_ID);
        }
    }

    private boolean AddPeerReference(PeerReference newPR) {

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference curRef = peerReferences.get(i);

            if (curRef != null) {
                continue;
            }

            LOGGER.log(Level.INFO, "{0} added peer {1}", new Object[]{peerID, newPR.getId()});

            peerReferences.set(i, newPR);

            return true;
        }

        return false;
    }

    private void DeletePeerReference(PeerReference deletedPR) {

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference curRef = peerReferences.get(i);

            if (curRef == null) {
                continue;
            }

            if (curRef.getId() == deletedPR.getId()) {
                peerReferences.remove(curRef);
                return;
            }
        }
    }

    public void SendMessage(int toPeerId, String msg) {
        Message message = new Message(msg);

        message.setTargetId(toPeerId);

        clientMessageQueue.add(message);
    }

    public void ConnectToId(int id) {
        if (connectedToOtherId == id) {
            LOGGER.log(Level.WARNING, "Peer {0} is already connected to {1}", new Object[]{peerID, id});
            return; 
        }
        
        if(tryingToConnectToOtherId == id)
        {
            //Already trying to connect to this id.
            return;
        }

        if (HasPeerReferenceId(id)) {
            ConnectToPeerId(id);
            LOGGER.log(Level.INFO, "Peer {0} will connect to id {1}", new Object[]{peerID, id});
            return;
        }

        PeerReference nextPR = FindShortestAvailablePeerRef();

        if (nextPR == null) {
            LOGGER.log(Level.SEVERE, "Unable to connect to a non existing peer reference!");
            return;
        }

        //If unable to connect to peer id, connect to shortest available peer
        //and send a search peer request to it to see if it has a peerReference to the original
        //peer id request.
        //ConnectToPeerId(nextPR.getId());

        PeerReference sourcePeerRef = new PeerReference(peerID, getAddress(), getPort());
        
        if(tryingToConnectToOtherId == nextPR.getId())
            return;

        SearchMessage searchRequestMsg = MessageParser.CreateSearchPeerMessage(nextPR.getId(), sourcePeerRef, id);

        LOGGER.log(Level.INFO, "Peer {0} will connect to {1} to search for target id {2}", new Object[]{peerID, nextPR.getId(), id});
        clientMessageQueue.addFirst(searchRequestMsg);
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

        if (tryingToConnectToOtherId == id) {
            return true;
        }
        
        PeerReference lastPeerRef = getLastPeerRequest();
        
        if(lastPeerRef != null && lastPeerRef.getId() == id)
        {
            tryingToConnectToOtherId = id;
            
            ConnectToAddress(lastPeerRef.getAddress(), lastPeerRef.getPortNumber());
            
            return true;
        }

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef.getId() != id) {
                continue;
            }

            tryingToConnectToOtherId = id;

            ConnectToAddress(peerRef.getAddress(), peerRef.getPortNumber());

            return true;
        }

        return false;
    }

    private boolean HasPeerReferenceId(int id) {
        
        if(getLastPeerRequest() != null && getLastPeerRequest().getId() == id){
            return true;
        }
        
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

    private PeerReference GetPeerReferenceById(int id) {
        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef == null) {
                continue;
            }

            if (peerRef.getId() == id) {
                return peerRef;
            }

        }

        return null;
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

        if (client == null) {
            return;
        }

        Message msgToSend = clientMessageQueue.peek();

        if (msgToSend != null) {

            //Special case; Close down client if we detect a QUIT message.
            if (clientIsConnected && msgToSend.getMessageType() == Constants.MSG_QUIT) {
                client.writeMessage(clientMessageQueue.poll());

                clientMessageQueue.removeIf(p -> p.getMessageType() == Constants.MSG_QUIT);
                connectedToOtherId = -1;
                tryingToConnectToOtherId = -1;
                return;
            }

            if (msgToSend.getTargetId() != connectedToOtherId) {
                LOGGER.log(Level.INFO, "Peer {0} is connected to {1} while trying to connect to {2}", new Object[]{peerID, connectedToOtherId, msgToSend.getTargetId()});
                ConnectToId(msgToSend.getTargetId());
            } else {
                client.writeMessage(clientMessageQueue.poll());

                //DisconnectPeerFromOtherPeer(false);
            }
        }

    }

    private void AddBootPeer() {
        PeerReference bootPeerRef = new PeerReference(Constants.BOOTPEER_ID, "localhost", Constants.SERVERPORT);
        if (AddPeerReference(bootPeerRef)) {
            LOGGER.log(Level.INFO, "Registered boot peer reference.");
        }
    }

    private void DeleteBootPeer() {
        PeerReference bootPeerRef = new PeerReference(Constants.BOOTPEER_ID, "localhost", Constants.SERVERPORT);
        DeletePeerReference(bootPeerRef);
        LOGGER.log(Level.INFO, "Deleted boot peer reference.");
    }

    public void JoinNetwork() {

        if (peerID == Constants.DISCONNECTED_PEERID) {
            RequestPeerId(Constants.BOOTPEER_ID);
        }

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

    private void RequestPeerReferences(int connectionId) {
        int idToFind = -1;
        PeerReference ownRef = new PeerReference(peerID, getAddress(), getPort());

        for (int i = 0; i < peerReferences.size(); i++) {

            idToFind = Math.floorMod((int) (peerID + Math.pow(2, i)), Constants.P2PSIZE);

            if (HasPeerReferenceId(idToFind)) {
                continue;
            }

            SearchMessage requestPeerMessage = MessageParser.CreateSearchPeerMessage(connectionId, ownRef, idToFind);

            clientMessageQueue.add(requestPeerMessage);
        }

    }

    private void RequestPeerId(int connectionId) {
        Message peerRequestMessage = MessageParser.CreatePeerIDRequest(connectionId);
        clientMessageQueue.add(peerRequestMessage);
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

    public String getPeerReferences() {
        String readablePeerReferences = "";
        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            readablePeerReferences += peerRef == null ? " [NULL] " : " [" + peerRef.getId() + "] ";
        }

        return readablePeerReferences;
    }

    private PeerReference getLastPeerRequest() {
        return lastPeerRequest;
    }

    private void setLastPeerRequest(PeerReference newLastPeerRef)
    {
        if(newLastPeerRef == lastPeerRequest)
            return;
        
        LOGGER.log(Level.INFO, "Peer {0} added {1} as last peer request.", new Object[]{peerID, newLastPeerRef});
        
        lastPeerRequest = newLastPeerRef;
    }
    
    @Override
    public void OnClientStarted() {
        LOGGER.log(Level.INFO, "Client {0} has started!", peerID);
    }

    @Override
    public void OnClientConnectedToServer() {
        clientIsConnected = true;

        connectedToOtherId = tryingToConnectToOtherId;

        tryingToConnectToOtherId = -1;
        LOGGER.log(Level.INFO, "Client {0} connected to peer " + connectedToOtherId, peerID);
    }

    @Override
    public void OnClientDisconnected() {
        clientIsConnected = false;

        connectedToOtherId = -1;
        LOGGER.log(Level.INFO, "Client {0} has disconnected!", peerID);
    }

    @Override
    public void OnClientError() {

        if (client != null) {
            client.StopConnection();
        }

        LOGGER.log(Level.SEVERE, "Client {0} has an error!", peerID);
    }

    @Override
    public void OnClientSentMessage(JSONObject jsonObj) {
        LOGGER.log(Level.INFO, "Client " + peerID + " has sent {0}", jsonObj);
    }

    @Override
    public void OnClientRecievedMessage() {

        IMessage recievedMsg = client.getMessage();

        switch (recievedMsg.getMessageType()) {
            case Constants.MSG_MESSAGE:
                LOGGER.log(Level.INFO, "Client " + peerID + " recieved message = {0}", recievedMsg.getMsg());
                break;

            case Constants.MSG_PEERID:

                String recievedString = recievedMsg.getMsg();
                Byte recievedId = Byte.parseByte(recievedString);
                SetID(recievedId);

                //After recieving a peerid, request to join the network.
                JoinMessage JoinPeerMsg = MessageParser.CreateJoinPeerMessage(Constants.BOOTPEER_ID, peerID, getAddress(), getPort());

                clientMessageQueue.add(JoinPeerMsg);

                LOGGER.log(Level.INFO, "Client " + peerID + " recieved peer id = {0}", recievedString);
                break;

            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Client {0} recieved quit command!", peerID);
                //Stop();
                break;

            case Constants.MSG_JOIN:

                //After succesfully recieved an ACK from joining -> Disconnect
                //DisconnectPeerFromOtherPeer(false);
                break;

            case Constants.MSG_RESPONSE_CONNECTIONINFO:

                break;

            case Constants.MSG_RESPONSE_SEARCH_PEERREF:

                SearchMessage searchResponse = (SearchMessage) recievedMsg;

                PeerReference newlyAcuiredPeerRef = searchResponse.getTargetPeerReference();

                AddPeerReference(newlyAcuiredPeerRef);

                LOGGER.log(Level.INFO, "Client {0} added {1}", new Object[]{peerID, newlyAcuiredPeerRef});

                break;
        }
    }

    @Override
    public void OnServerStarted() {
        LOGGER.log(Level.INFO, "Server {0} started!", peerID);
    }

    @Override
    public void OnServerAcceptedConnection() {
        LOGGER.log(Level.INFO, "Server {0} has accepted incoming connection!", peerID);
    }

    @Override
    public void OnServerError() {
        LOGGER.log(Level.SEVERE, "Server {0} has an error!", peerID);

    }

    @Override
    public void OnServerSentMessage(JSONObject jsonObj) {
        LOGGER.log(Level.INFO, "Server " + peerID + " has sent {0}", jsonObj);
    }

    @Override
    public void OnServerRecievedMessage() {

        IMessage recievedMsg = server.getMessage();
        
        if(processedGuids.contains(recievedMsg.getGuid()))
        {
            LOGGER.log(Level.INFO, "Server already processed message {0}", recievedMsg.getMsg());
            return;
        }

        LOGGER.log(Level.INFO, "Server " + peerID +  " recieved {0}", recievedMsg.getMsg());

        switch (recievedMsg.getMessageType()) {
            case Constants.MSG_MESSAGE:
                LOGGER.log(Level.INFO, "Server " + peerID + " recieved message = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_REQUEST_CONNECTIONINFO:

                //Other peer is requesting direct ip address
                SearchMessage recievedSearchRequest = (SearchMessage) recievedMsg;
                PeerReference targetPeerRef = recievedSearchRequest.getTargetPeerReference();
                PeerReference sourcePeerRef = recievedSearchRequest.getSourcePeerReference();

                if (sourcePeerRef.getId() == peerID) {
                    LOGGER.log(Level.INFO, "Recieved response! I got information from other peers to {0}", targetPeerRef);
                    break;
                }

                int searchedForId = targetPeerRef.getId();

                if (HasPeerReferenceId(searchedForId)) {

                    // If we have got the peerreference, directly send the info back to the source.
                    targetPeerRef = GetPeerReferenceById(searchedForId);
                    SearchMessage foundTargetPeerMessage = MessageParser.CreateSearchPeerAddressFoundMessage(sourcePeerRef.getId(), sourcePeerRef, targetPeerRef);

                    clientMessageQueue.add(foundTargetPeerMessage);
                    break;
                }

                PeerReference shortestNextPR = FindShortestAvailablePeerRef();
                if (shortestNextPR == null) {
                    LOGGER.log(Level.WARNING, "Unable to find an other active peer :(");
                    break;
                }

                SearchMessage searchingForTargetPeer = MessageParser.CreateSearchPeerAddressMessage(shortestNextPR.getId(), sourcePeerRef, targetPeerRef.getId());

                clientMessageQueue.add(searchingForTargetPeer);

                break;
            case Constants.MSG_PEERID:
                //LOGGER.log(Level.INFO, "Server recieved peer id = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Server {0} recieved quit command!", peerID);
                server.StopConnection(Integer.parseInt(recievedMsg.getMsg()));
                break;
            case Constants.MSG_REQUEST_PEERID:

                for (int i = 0; i < availablePeerIds.size(); i++) {
                    int assignedId = availablePeerIds.get(i);
                    server.writeMessage(MessageParser.CreatePeerIDMessage(assignedId));

                    availablePeerIds.remove(i);
                    break;
                }

                break;

            case Constants.MSG_JOIN:

                JoinMessage joinMessage = (JoinMessage) recievedMsg;

                if (AddPeerReference(joinMessage.getJoiningPeer())) {
                    LOGGER.log(Level.INFO, "Added peer reference {0}", joinMessage.getJoiningPeer());
                    break;
                }

                //Failed to add reference to this peer, go to next peer!
                PeerReference shortestAvailableRef = FindShortestAvailablePeerRef();

                joinMessage.setTargetId(shortestAvailableRef.getId());

                clientMessageQueue.add(joinMessage);

                LOGGER.log(Level.INFO, "Unable to add peer reference {0} , Informed shortest peer id " + shortestAvailableRef.getId(), joinMessage);
                break;

            case Constants.MSG_REQUEST_SEARCH_PEERREF:

                //Other peer is requesting a peerreference
                SearchMessage requestMsg = (SearchMessage) recievedMsg;

                int targetId = requestMsg.getTargetPeerReference().getId();

                if (HasPeerReferenceId(targetId)) {

                    //We have found the peerref in our references!
                    PeerReference foundRef = GetPeerReferenceById(targetId);
                    PeerReference sourceRef = requestMsg.getSourcePeerReference();

                    SearchMessage responseFoundMessage = MessageParser.CreateSearchPeerFoundMessage(sourceRef.getId(), sourceRef, foundRef);

                    LOGGER.log(Level.INFO, "Sending back peer reference {0}", foundRef);

                    //Inform the connection that we have succesfully found the peerRef
                    clientMessageQueue.add(responseFoundMessage);

                    break;
                }

                //Go to next peer!
                PeerReference shortestAvailableReference = FindShortestAvailablePeerRef();

                if (shortestAvailableReference == null) {
                    LOGGER.log(Level.WARNING, "Peer {0} was unable to find shortest available peer ref!", peerID);
                    break;
                }

                requestMsg.setTargetId(shortestAvailableReference.getId());

                clientMessageQueue.add(requestMsg);

                LOGGER.log(Level.INFO, "Unable to add peer reference {0} , Informed shortest peer id " + shortestAvailableReference.getId(), requestMsg);
                break;

            case Constants.MSG_RESPONSE_CONNECTIONINFO:
                SearchMessage searchConnectionResponse = (SearchMessage) recievedMsg;
                
                PeerReference responseConnectionInfo = searchConnectionResponse.getTargetPeerReference();
                
                setLastPeerRequest(responseConnectionInfo);
                
                break;

            case Constants.MSG_RESPONSE_SEARCH_PEERREF:

                SearchMessage searchResponse = (SearchMessage) recievedMsg;

                PeerReference newlyAcuiredPeerRef = searchResponse.getTargetPeerReference();

                if(!HasPeerReferenceId(newlyAcuiredPeerRef.getId()))
                {
                    
                if(!AddPeerReference(newlyAcuiredPeerRef))
                {
                    setLastPeerRequest(newlyAcuiredPeerRef);
                    break;
                }

                LOGGER.log(Level.INFO, "Server {0} added {1}", new Object[]{peerID, newlyAcuiredPeerRef});
                }
                break;

        }

        processedGuids.add(recievedMsg.getGuid());
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
