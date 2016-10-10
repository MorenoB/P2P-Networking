package Util;

import Interfaces.IMessage;
import data.JoinMessage;
import data.Message;
import data.PeerReference;
import data.SearchMessage;
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

        try {

            sourcePeerRef = DecodeJsonToPeerReference(jsonObj.getJSONObject("sourcePeerReference"));
            targetPeerRef = DecodeJsonToPeerReference(jsonObj.getJSONObject("targetPeerReference"));

        } catch (Exception e) {

        }

        switch (messageType) {
            case Constants.MSG_JOIN:

                PeerReference joiningPeer = DecodeJsonToPeerReference(jsonObj.getJSONObject("joiningPeer"));

                JoinMessage joinMessage = new JoinMessage(joiningPeer);

                joinMessage.setGuid(messageGuid);
                joinMessage.setTargetId(targetId);

                return joinMessage;

            case Constants.MSG_REQUEST_CONNECTIONINFO:

                SearchMessage searchReqConnection = CreateSearchPeerAddressMessage(targetId, sourcePeerRef, targetPeerRef.getId());

                searchReqConnection.setGuid(messageGuid);
                return searchReqConnection;

            case Constants.MSG_REQUEST_SEARCH_PEERREF:

                SearchMessage searchReqPeerRef = CreateSearchPeerMessage(targetId, sourcePeerRef, targetPeerRef.getId());

                searchReqPeerRef.setGuid(messageGuid);
                return searchReqPeerRef;

            case Constants.MSG_RESPONSE_CONNECTIONINFO:
                SearchMessage searchResponseConnection = CreateSearchPeerAddressFoundMessage(targetId, sourcePeerRef, targetPeerRef);

                searchResponseConnection.setGuid(messageGuid);
                return searchResponseConnection;
            case Constants.MSG_RESPONSE_SEARCH_PEERREF:
                SearchMessage searchResponsePeerRef = CreateSearchPeerFoundMessage(targetId, sourcePeerRef, targetPeerRef);

                searchResponsePeerRef.setGuid(messageGuid);
                return searchResponsePeerRef;
            default:
                Message message = new Message(messageType);
                message.setMsg(msg);
                message.setTargetId(targetId);
                message.setGuid(messageGuid);

                return message;
        }

        /* if (messageType == Constants.MSG_JOIN) {
            //,"address":"1201","messageType":6,"id":-1,"portNumber":1201
            String address = jsonObj.getString("address");
            int portNumber = jsonObj.getInt("portNumber");
            message = new Constants(messageId, address, portNumber);

            message.setMsg(msg);
        }*/
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

    public static SearchMessage CreateSearchPeerAddressMessage(int targetConnectionId, PeerReference sourcePeerRef, int id) {
        SearchMessage message = new SearchMessage(true, sourcePeerRef, id);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static SearchMessage CreateSearchPeerAddressFoundMessage(int targetConnectionId, PeerReference sourcePeerRef, PeerReference targetPeerRef) {
        SearchMessage message = new SearchMessage(true, sourcePeerRef, targetPeerRef);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static SearchMessage CreateSearchPeerMessage(int targetConnectionId, PeerReference sourcePeerRef, int id) {
        SearchMessage message = new SearchMessage(false, sourcePeerRef, id);

        message.setTargetId(targetConnectionId);

        return message;
    }

    public static SearchMessage CreateSearchPeerFoundMessage(int targetConnectionId, PeerReference sourcePeerRef, PeerReference targetPeerRef) {
        SearchMessage message = new SearchMessage(false, sourcePeerRef, targetPeerRef);

        message.setTargetId(targetConnectionId);

        return message;
    }
}
