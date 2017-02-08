package p2pnetwork;

import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
import Interfaces.IPeerListener;
import Util.Constants;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import data.JoinMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import data.Message;
import data.PeerReference;
import data.RoutingTableMessage;
import data.FindClosestMessage;
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
    public final List<IPeerListener> peerListeners = new ArrayList<>();

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

    private Message lastRecievedMessage;

    private final ConcurrentLinkedDeque<Message> clientMessageQueue;

    private Constants.PEER_STATUS peerStatus;

    public Peer(byte peerID, int port) {

        this.clientMessageQueue = new ConcurrentLinkedDeque<>();

        this.availablePeerIds = new ArrayList<>();
        this.isRunning = true;

        this.peerID = peerID;
        this.port = port;

        this.peerReferences = new ArrayList<>();
        this.processedGuids = new ArrayList<>();

        this.peerStatus = Constants.PEER_STATUS.IDLE;
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

        ResetPeerReferencesList();

        server = new Server(port);

        client = new Client();

        server.AddListener(this);
        client.AddListener(this);

        Thread serverThread = new Thread(server, "ServerThead");
        Thread clientThread = new Thread(client, "ClientThread");

        //Start Threads
        serverThread.start();

        clientThread.start();
    }
    
    private void ResetPeerReferencesList()
    {
        int peerReferencesSize = isBootpeer() ? Constants.P2PSIZE : Constants.PEERREFERENCE_SIZE;

        if(peerReferences.size() > 0)
            peerReferences.clear();
        
        for (int i = 0; i < peerReferencesSize; i++) {
            peerReferences.add(null);
        }
    }

    private void ConnectToAddress(String ipAddress, int port) {
        if (client.HasConnection()) {
            LOGGER.log(Level.WARNING, "Peer {0} already has a connection while setting up a new connection!", peerID);
            DisconnectPeerFromOtherPeer(true);
        }
        client.SetupConnection(ipAddress, port);
    }

    private void DisconnectPeerFromOtherPeer(boolean instant) {
        //Notify server we want to close connection.

        Message quitMesssage = MessageParser.CreateQuitMessage(connectedToOtherId, port);

        if (instant) {
            //client.writeMessage(quitMesssage);
            client.StopConnection();
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
        if(getId() == newId)
            return;
        
        LOGGER.log(Level.INFO, "Peer " + peerID + " set peerId to {0}", newId);
        peerID = newId;
    }

    private boolean AddPeerReference(PeerReference newPR) {

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference curRef = peerReferences.get(i);

            if (curRef != null) {
                continue;
            }

            LOGGER.log(Level.INFO, "Peer {0} added peer {1}", new Object[]{peerID, newPR.getId()});

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

    private PeerReference GetOwnPeerReference() {
        PeerReference ownRef = new PeerReference(getId(), getAddress(), getPort());
        return ownRef;
    }

    private void SendMessageToPeerReference(Message message, PeerReference peerReference) {
        ConnectToAddress(peerReference.getAddress(), peerReference.getPortNumber());
        client.writeMessage(message);
    }

    public void ConnectToId(int id) {
        if (connectedToOtherId == id) {
            LOGGER.log(Level.WARNING, "Peer {0} is already connected to {1}", new Object[]{peerID, id});
            return;
        }

        if (tryingToConnectToOtherId == id) {
            //Already trying to connect to this id.
            return;
        }

        if (ConnectToPeerIdFromRoutingTable(id)) {
            //LOGGER.log(Level.INFO, "Peer {0} will connect to id {1}", new Object[]{peerID, id});
            return;
        }

        PeerReference nextPR = FindClosestPeerReference(id);

        if (nextPR == null) {
            LOGGER.log(Level.SEVERE, "Peer {0} unable to connect to a non existing peer reference!", peerID);
            return;
        }

        //If unable to connect to peer id, connect to shortest available peer
        //and send a search peer request to it to see if it has a peerReference to the original
        //peer id request.
        //ConnectToPeerId(nextPR.getId());
        PeerReference sourcePeerRef = new PeerReference(peerID, getAddress(), getPort());

        if (tryingToConnectToOtherId == nextPR.getId()) {
            return;
        }
        
        FindClosestMessage searchRequestMsg = MessageParser.CreateSearchPeerMessage(nextPR.getId(), sourcePeerRef, id);

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
    private boolean ConnectToPeerIdFromRoutingTable(int id) {

        if (tryingToConnectToOtherId == id) {
            return true;
        }

        PeerReference lastPeerRef = getLastPeerRequest();

        if (lastPeerRef != null && lastPeerRef.getId() == id) {
            tryingToConnectToOtherId = id;

            ConnectToAddress(lastPeerRef.getAddress(), lastPeerRef.getPortNumber());

            return true;
        }

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef == null) {
                continue;
            }

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

        if (getLastPeerRequest() != null && getLastPeerRequest().getId() == id) {
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

    private PeerReference FindClosestPeerReference(int targetId) {
        int lastDistance = Integer.MAX_VALUE;
        int shortestPeerRefId = Integer.MAX_VALUE;

        for (int i = 0; i < peerReferences.size(); i++) {
            PeerReference peerRef = peerReferences.get(i);

            if (peerRef == null) {
                continue;
            }

            int distanceOfPeerReference = CalculateDistance(peerRef.getId(), targetId);
            if (distanceOfPeerReference < lastDistance) {
                shortestPeerRefId = i;
            }
        }
        if (shortestPeerRefId == Integer.MAX_VALUE) {
            return GetOwnPeerReference();
        } else {
            return peerReferences.get(shortestPeerRefId);
        }
    }

    private int CalculateDistance(int srcID, int destID) {

        int result = (destID - srcID + (int) Math.pow(2, Constants.P2PSIZE)) % (int) Math.pow(2, Constants.P2PSIZE);

        return result;
    }

    private void Update() {

        if (client == null) {
            return;
        }

        Message msgToSend = clientMessageQueue.peek();

        if (msgToSend != null) {

            if (msgToSend.getTargetId() == getId()) {
                LOGGER.log(Level.WARNING, "Peer {0} is trying to send the following message to itself : {1}", new Object[]{getId(), msgToSend.getMsg()});
                clientMessageQueue.poll();
                return;
            }

            //Special case; Close down client if we detect a QUIT message.
            if (clientIsConnected && msgToSend.getMessageType() == Constants.MSG_QUIT) {
                client.writeMessage(clientMessageQueue.poll());

                clientMessageQueue.removeIf(p -> p.getMessageType() == Constants.MSG_QUIT);
                connectedToOtherId = -1;
                tryingToConnectToOtherId = -1;
                return;
            }

            if (peerStatus == Constants.PEER_STATUS.WAITINGFORCLOSESTRESPONSE) {
                return;
            }

            if (msgToSend.getTargetId() != connectedToOtherId) {
                LOGGER.log(Level.INFO, "Peer {0} is connected to {1} while trying to connect to {2}", new Object[]{peerID, connectedToOtherId, msgToSend.getTargetId()});
                ConnectToId(msgToSend.getTargetId());
            } else {
                
                client.writeMessage(clientMessageQueue.poll());
                
                if(msgToSend.getMessageType() == Constants.MSG_REQUEST_SEARCH_PEERREF)
                    peerStatus = Constants.PEER_STATUS.WAITINGFORCLOSESTRESPONSE;
                
            }
        }

    }

    private void AddBootPeer() {
        PeerReference bootPeerRef = new PeerReference(Constants.BOOTPEER_ID, "localhost", Constants.SERVERPORT);
        setLastPeerRequest(bootPeerRef);

        LOGGER.log(Level.INFO, "Peer {0} registered boot peer reference.", peerID);

    }

    public void JoinNetwork() {

        if (isDisconnectedFromNetwork()) {
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

    private void FillRoutingTable(List<PeerReference> routingTableCopy) {
        List<Integer> idsToFind = new ArrayList();
        
        ResetPeerReferencesList();
        
        for (int i = 0; i < peerReferences.size(); i++) {
            
            int idToFind = (int) (peerID + Math.pow(2, i));
                        
            if(idToFind >= Constants.P2PSIZE)
                idToFind = Math.floorMod(idToFind, Constants.P2PSIZE);
            
            //Make sure the top p2psize peer id is not being chosen because this is the boot peer id!
            if(idToFind == Constants.BOOTPEER_ID)
                idToFind = 0;

            if (HasPeerReferenceId(idToFind)) {
                continue;
            }

            idsToFind.add(idToFind);
        }

        //Fill our routing table according to matching ids.
        for (int i = 0; i < idsToFind.size(); i++) {
            for (int j = 0; j < routingTableCopy.size(); j++) {
                PeerReference peerRef = routingTableCopy.get(j);
                
                int idToFind = idsToFind.get(i);

                if (peerRef.getId() == idToFind) {
                    AddPeerReference(peerRef);
                    idsToFind.remove(i);
                    break;
                }
            }
        }

        if (idsToFind.isEmpty()) {
            return;
        }

        FillEmptyRoutingTableSpotsWithNearestIds(idsToFind, routingTableCopy);
    }
    
    private boolean RoutingTableContainsId(List<PeerReference> routingTable, int id)
    {
        for (int i = 0; i < routingTable.size(); i++) {
            PeerReference peerRef = routingTable.get(i);
            if(peerRef == null) continue;
            
            if(peerRef.getId() == id)
                return true;
        }
        return false;
    }

    public String GetPeerStatus() {
        return peerStatus.toString();
    }

    private void FillEmptyRoutingTableSpotsWithNearestIds(List<Integer> idsToFind, List<PeerReference> routingTableCopy) {
        for (int i = 0; i < idsToFind.size(); i++) {
            int idToFind = idsToFind.get(i);

            PeerReference peerRef = FindNextAvailablePeerReferenceRecursive(routingTableCopy, idToFind, 0);

            if (peerRef != null) {
                AddPeerReference(peerRef);
            }
        }
    }
    
    private PeerReference FindNextAvailablePeerReferenceRecursive(List<PeerReference> routingTable, int id, int distance)
    {
        int idToFind = id + distance;
        boolean alreadyHasPeerReference = false;
        
        if(idToFind >= Constants.P2PSIZE)
            idToFind = Math.floorMod(idToFind, Constants.P2PSIZE);
        
        
        if(!RoutingTableContainsId(routingTable, idToFind))
        {
            return FindNextAvailablePeerReferenceRecursive(routingTable, id, distance + 1);
        }
        
        for (int i = 0; i < routingTable.size(); i++) {
            PeerReference peerRef = routingTable.get(i);
            if(peerRef == null) continue;
            
            if(peerRef.getId() == getId())
                continue;
            
            if(HasPeerReferenceId(peerRef.getId()))
            {
                alreadyHasPeerReference = true;
                continue;
            }
            
            if(peerRef.getId() == idToFind)
                return peerRef;
        }
        
        if(alreadyHasPeerReference && routingTable.size() > Constants.PEERREFERENCE_SIZE)
            return FindNextAvailablePeerReferenceRecursive(routingTable, id, distance + 1);
        
        return null;
    }

    private void SendCopyOfRoutingTableToAllReferences()
    {
        for (int i = 0; i < peerReferences.size(); i++) {
            
            if(peerReferences.get(i) == null) continue;
            
            int targetPeerId = peerReferences.get(i).getId();

            List<PeerReference> routingTableCopy = new ArrayList<>();
            for (int j = 0; j < peerReferences.size(); j++) {
                PeerReference peerRef = peerReferences.get(j);

                if (peerRef == null) {
                    continue;
                }

                routingTableCopy.add(peerRef);
            }

            RoutingTableMessage routingTableResponse = MessageParser.CreateRoutingTableResponse(targetPeerId, routingTableCopy);

            clientMessageQueue.add(routingTableResponse);
        }
}

    private void RequestPeerId(int connectionId) {
        peerStatus = Constants.PEER_STATUS.JOINING;

        JoinMessage JoinPeerMsg = MessageParser.CreateJoinPeerMessage(Constants.BOOTPEER_ID, getId(), getAddress(), getPort());

        clientMessageQueue.add(JoinPeerMsg);
    }

    private boolean isDisconnectedFromNetwork() {
        return peerID == Constants.DISCONNECTED_PEERID || bootPeer;
    }

    public void setBootPeer(boolean active) {
        bootPeer = active;

        SetID((byte) Constants.BOOTPEER_ID);
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

    public boolean isBootpeer() {
        return bootPeer;
    }

    public ConcurrentLinkedDeque GetMessageQueue() {
        return clientMessageQueue;
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

    public Message getLastRecievedMessage() {
        return lastRecievedMessage;
    }

    private int getNextAvailableId() {
        for (int i = 0; i < availablePeerIds.size(); i++) {
            int assignedId = availablePeerIds.get(i);
            availablePeerIds.remove(i);

            return assignedId;
        }
        return Constants.DISCONNECTED_PEERID;

    }

    private void setLastPeerRequest(PeerReference newLastPeerRef) {
        if (newLastPeerRef == lastPeerRequest) {
            return;
        }

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
        peerListeners.stream().forEach((sl) -> {
            sl.OnMessageSent();
        });
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
    public void OnServerError(int portNr) {
        LOGGER.log(Level.SEVERE, "Server {0} has an error!", peerID);

        if (portNr == -1) {
            return;
        }

        server.StopConnection(portNr);

    }

    @Override
    public void OnServerRecievedMessage() {

        IMessage recievedMsg = server.getMessage();

        if (processedGuids.contains(recievedMsg.getGuid())) {
            LOGGER.log(Level.INFO, "Server " + peerID + " already processed message {0}", recievedMsg.getMsg());
            return;
        }

        switch (recievedMsg.getMessageType()) {
            case Constants.MSG_MESSAGE:
                LOGGER.log(Level.INFO, "Server " + peerID + " recieved message = {0}", recievedMsg.getMsg());
                break;
            case Constants.MSG_PEERID_RESPONSE:
                //LOGGER.log(Level.INFO, "Server recieved peer id = {0}", recievedMsg.getMsg());
                if (isBootpeer()) {
                    break;
                }

                String recievedString = recievedMsg.getMsg();
                Byte recievedId = Byte.parseByte(recievedString);
                SetID(recievedId);

                break;
            case Constants.MSG_QUIT:
                LOGGER.log(Level.SEVERE, "Server {0} recieved quit command!", peerID);
                server.StopConnection(Integer.parseInt(recievedMsg.getMsg()));
                break;

            case Constants.MSG_JOIN:

                // Only accepted by bootpeer
                if (!isBootpeer()) {
                    return;
                }

                JoinMessage joinMessage = (JoinMessage) recievedMsg;

                PeerReference joiningPeer = joinMessage.getJoiningPeer();

                int newId = getNextAvailableId();
                
                PeerReference newlyAddedPeer = new PeerReference(newId, joiningPeer.getAddress(), joiningPeer.getPortNumber());

                if (AddPeerReference(newlyAddedPeer)) {
                    LOGGER.log(Level.INFO, "Added peer reference {0}", newlyAddedPeer);
                }
                
                clientMessageQueue.add(MessageParser.CreatePeerIDMessage(newId));
                
                //We have noticed an update in peer references
                SendCopyOfRoutingTableToAllReferences();

                break;

            case Constants.MSG_REQUEST_SEARCH_PEERREF:

                peerStatus = Constants.PEER_STATUS.RESPONDING;

                //Other peer is requesting a peerreference
                FindClosestMessage requestMsg = (FindClosestMessage) recievedMsg;

                int targetId = requestMsg.getTargetPeerReference().getId();
                PeerReference sourceRef = requestMsg.getSourcePeerReference();

                if (HasPeerReferenceId(targetId)) {

                    //We have found the peerref in our references!
                    PeerReference foundRef = GetPeerReferenceById(targetId);

                    if (foundRef == null) {
                        LOGGER.log(Level.WARNING, "Peer {0} does not have a full reference to id {1}", new Object[]{peerID, targetId});
                        break;
                    }

                    FindClosestMessage responseFoundMessage = MessageParser.CreateSearchPeerFoundMessage(sourceRef.getId(), sourceRef, foundRef);

                    LOGGER.log(Level.INFO, "Peer " + peerID + " sending back peer reference with id {0}", foundRef.getId());

                    responseFoundMessage.setHasTargetReference(true);

                    //Inform the connection that we have succesfully found the peerRef
                    clientMessageQueue.add(responseFoundMessage);

                    break;
                }

                //Go to next peer!
                PeerReference shortestAvailableReference = FindClosestPeerReference(targetId);

                if (shortestAvailableReference == null) {
                    LOGGER.log(Level.WARNING, "Peer {0} was unable to find shortest available peer ref!", peerID);
                    break;
                }

                FindClosestMessage responseNotFoundMessage = MessageParser.CreateSearchPeerFoundMessage(sourceRef.getId(), sourceRef, shortestAvailableReference);
                responseNotFoundMessage.setHasTargetReference(false);
                clientMessageQueue.add(responseNotFoundMessage);

                LOGGER.log(Level.INFO, "Peer " + peerID + " does not have target search peer req id {0} , Informed shortest peer id " + shortestAvailableReference.getId(), targetId);
                break;

            case Constants.MSG_RESPONSE_SEARCH_PEERREF:

                FindClosestMessage searchResponse = (FindClosestMessage) recievedMsg;

                PeerReference newlyAcuiredPeerRef = searchResponse.getTargetPeerReference();

                int originalSearchId = searchResponse.getOriginalSearchId();
                //If we have the target peer ref, connect to it
                if (searchResponse.getHasTargetReference()) {

                    Message debugMsg = new Message("NOT IMPLEMENTED");
                    
                    if(clientMessageQueue.peek().getMessageType() == Constants.MSG_MESSAGE)
                    {
                        debugMsg = clientMessageQueue.poll();
                    }
                    else if(clientMessageQueue.peek().getMessageType() == Constants.MSG_REQUEST_SEARCH_PEERREF)
                    {
                        clientMessageQueue.poll();
                        debugMsg = clientMessageQueue.poll();
                    }
                    
                    SendMessageToPeerReference(debugMsg ,newlyAcuiredPeerRef);

                    peerStatus = Constants.PEER_STATUS.SENDINGMSG;
                    
                    
                    break;
                }
                
                LOGGER.log(Level.INFO, "Peer {0} does not have received the target peer ref, will connect to {1} instead." , new Object[]{ getId(), newlyAcuiredPeerRef.getId() });

                PeerReference myPeerRef = new PeerReference(peerID, getAddress(), getPort());

                //If we haven't recieved the target peer ref, connect to the recommended recieved peer ref and send the same search request.
                FindClosestMessage searchRequestMsg = MessageParser.CreateSearchPeerMessage(newlyAcuiredPeerRef.getId(), myPeerRef, originalSearchId);

                lastPeerRequest = newlyAcuiredPeerRef;
 
                SendMessageToPeerReference(searchRequestMsg, newlyAcuiredPeerRef);

                break;

            case Constants.MSG_RESPONSE_ROUTINGTABLE:

                RoutingTableMessage incomingRoutingTableResponse = (RoutingTableMessage) recievedMsg;
                
                SetID((byte)incomingRoutingTableResponse.getTargetId());

                FillRoutingTable(incomingRoutingTableResponse.getRoutingTableCopy());
                peerStatus = Constants.PEER_STATUS.IDLE;

                break;

        }

        LOGGER.log(Level.INFO, "Peer {0} recieved {1}", new Object[]{peerID, recievedMsg.getMsg()});
        lastRecievedMessage = (Message) recievedMsg;
        processedGuids.add(recievedMsg.getGuid());
        
        peerListeners.stream().forEach((sl) -> {
            sl.OnMessageReceived();
        });
    }

    @Override
    public void run() {

        while (!server.isRunning() || !server.isReady()) {
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        if(!isBootpeer())
            JoinNetwork();

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
