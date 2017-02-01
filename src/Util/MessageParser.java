package Util;

import Interfaces.IMessage;
import data.JoinMessage;
import data.Message;
import data.PeerReference;
import data.RoutingTableMessage;
import data.FindClosestMessage;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public final class MessageParser {

    public static IMessage DecodeJSON(String rawJson) {

        JSONObject jsonObj = new JSONObject(rawJson);

        String msg = jsonObj.getString("msg");
        int messageType = jsonObj.getInt("messageType");
        String messageGuid = jsonObj.getString("guid");
        int targetId = jsonObj.getInt("targetId");

        PeerReference sourcePeerRef = null;
        PeerReference targetPeerRef = null;
        
        boolean hasTargetRef = false;
        int originalSearchForId = -1;

        try {

            sourcePeerRef = DecodeJsonToPeerReference(jsonObj.getJSONObject("sourcePeerReference"));
            targetPeerRef = DecodeJsonToPeerReference(jsonObj.getJSONObject("targetPeerReference"));
            
            hasTargetRef = jsonObj.getBoolean("hasTargetReference");
            originalSearchForId = jsonObj.getInt("originalSearchId");

        } catch (JSONException e) {
        }

        switch (messageType) {
            case Constants.MSG_JOIN:

                PeerReference joiningPeer = DecodeJsonToPeerReference(jsonObj.getJSONObject("joiningPeer"));

                JoinMessage joinMessage = new JoinMessage(joiningPeer);

                joinMessage.setGuid(messageGuid);
                joinMessage.setTargetId(targetId);

                return joinMessage;

            case Constants.MSG_REQUEST_CONNECTIONINFO:

                FindClosestMessage searchReqConnection = CreateSearchPeerAddressMessage(targetId, sourcePeerRef, targetPeerRef.getId());

                searchReqConnection.setGuid(messageGuid);
                return searchReqConnection;

            case Constants.MSG_REQUEST_SEARCH_PEERREF:

                FindClosestMessage searchReqPeerRef = CreateSearchPeerMessage(targetId, sourcePeerRef, targetPeerRef.getId());

                searchReqPeerRef.setGuid(messageGuid);
                
                searchReqPeerRef.setOriginalSearchId(originalSearchForId);
                
                searchReqPeerRef.setHasTargetReference(hasTargetRef);
                
                return searchReqPeerRef;

            case Constants.MSG_RESPONSE_CONNECTIONINFO:
                FindClosestMessage searchResponseConnection = CreateSearchPeerAddressFoundMessage(targetId, sourcePeerRef, targetPeerRef);

                searchResponseConnection.setGuid(messageGuid);
                return searchResponseConnection;
            case Constants.MSG_RESPONSE_SEARCH_PEERREF:
                FindClosestMessage searchResponsePeerRef = CreateSearchPeerFoundMessage(targetId, sourcePeerRef, targetPeerRef);

                searchResponsePeerRef.setGuid(messageGuid);
                
                searchResponsePeerRef.setOriginalSearchId(originalSearchForId);
                
                searchResponsePeerRef.setHasTargetReference(hasTargetRef);
                
                return searchResponsePeerRef;
                
            case Constants.MSG_REQUEST_ROUTINGTABLE:
                
                int sourceId = jsonObj.getInt("sourceId");
                
                RoutingTableMessage routingTableRequest = CreateRoutingTableRequest(sourceId);
                
                routingTableRequest.setGuid(messageGuid);
                
                return routingTableRequest;
                
            case Constants.MSG_RESPONSE_ROUTINGTABLE:
                
                JSONArray routingTableJSONArray = jsonObj.getJSONArray("routingTableCopy");
                List<PeerReference> routingTableCopyResponse = new ArrayList<>();
                for(int i = 0; i < routingTableJSONArray.length(); i++)
                {
                    if(routingTableJSONArray.get(i) == null) continue;
                    
                    routingTableCopyResponse.add(DecodeJsonToPeerReference(routingTableJSONArray.getJSONObject(i)));
                }
                
                RoutingTableMessage routingTableResponse = CreateRoutingTableResponse(targetId, routingTableCopyResponse);
                
                return routingTableResponse;
            default:
                Message message = new Message(messageType);
                message.setMsg(msg);
                message.setTargetId(targetId);
                message.setGuid(messageGuid);

                return message;
        }
    }

    public static PeerReference DecodeJsonToPeerReference(JSONObject jsonObj) {
        int id = jsonObj.getInt("id");
        int port = jsonObj.getInt("portNumber");
        String address = jsonObj.getString("address");
        PeerReference peerRef = new PeerReference(id, address, port);

        return peerRef;
    }

    public static Message CreatePeerIDMessage(int peerId) {
        Message message = new Message(Constants.MSG_PEERID);

        message.setMsg(Integer.toString(peerId));

        return message;
    }

    public static Message CreateQuitMessage(int targetConnectionId, int port) {
        Message message = new Message(Constants.MSG_QUIT);

        message.setMsg(Integer.toString(port));

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static Message CreatePeerIDRequest(int targetConnectionId) {
        Message message = new Message(Constants.MSG_REQUEST_PEERID);

        message.setMsg("IHRE PEER ID BITTE!");

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static JoinMessage CreateJoinPeerMessage(int targetConnectionId, int id, String address, int port) {
        PeerReference peerRef = new PeerReference(id, address, port);

        JoinMessage message = new JoinMessage(peerRef);

        message.setMsg("Joining peer request.");

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static FindClosestMessage CreateSearchPeerAddressMessage(int targetConnectionId, PeerReference sourcePeerRef, int id) {
        FindClosestMessage message = new FindClosestMessage(true, sourcePeerRef, id);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static FindClosestMessage CreateSearchPeerAddressFoundMessage(int targetConnectionId, PeerReference sourcePeerRef, PeerReference targetPeerRef) {
        FindClosestMessage message = new FindClosestMessage(true, sourcePeerRef, targetPeerRef);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static FindClosestMessage CreateSearchPeerMessage(int targetConnectionId, PeerReference sourcePeerRef, int id) {
        FindClosestMessage message = new FindClosestMessage(false, sourcePeerRef, id);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static FindClosestMessage CreateSearchPeerFoundMessage(int targetConnectionId, PeerReference sourcePeerRef, PeerReference targetPeerRef) {
        FindClosestMessage message = new FindClosestMessage(false, sourcePeerRef, targetPeerRef);

        message.setTargetId(targetConnectionId);

        return message;
    }
    
    public static RoutingTableMessage CreateRoutingTableRequest(int sourceId)
    {
        RoutingTableMessage routingTableRequestMsg = new RoutingTableMessage(sourceId);
        
        routingTableRequestMsg.setTargetId(Constants.BOOTPEER_ID);
        
        return routingTableRequestMsg;
    }
    
    public static RoutingTableMessage CreateRoutingTableResponse(int targetId, List<PeerReference> routingTableCopy)
    {
        RoutingTableMessage routingTableRequestMsg = new RoutingTableMessage(routingTableCopy);
        
        routingTableRequestMsg.setTargetId(targetId);
        
        return routingTableRequestMsg;
    }
}
