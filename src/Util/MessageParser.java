package Util;

import Interfaces.IMessage;
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
        int messageId = jsonObj.getInt("id");

        Message message = new Message(messageType, messageId);
        message.setMsg(msg);

        if (messageType == Constants.MSG_JOIN) {
            //,"address":"1201","messageType":6,"id":-1,"portNumber":1201
            String address = jsonObj.getString("address");
            int portNumber = jsonObj.getInt("portNumber");
            message = new PeerReference(messageId, address, portNumber);

            message.setMsg(msg);
        }

        return message;
    }

    public static Message CreatePeerIDMessage(int peerId) {
        Message message = new Message(Constants.MSG_PEERID);

        message.setMsg(Integer.toString(peerId));

        return message;
    }

    public static Message CreateQuitMessage() {
        Message message = new Message(Constants.MSG_QUIT);

        message.setMsg("SHUTDOWN");
        
        message.setTargetId(-1);

        return message;
    }

    public static Message CreatePeerIDRequest(int targetConnectionId) {
        Message message = new Message(Constants.MSG_REQUEST_PEERID);

        message.setMsg("IHRE PEER ID BITTE!");
        
        message.setTargetId(targetConnectionId);

        return message;
    }

    public static PeerReference CreateJoinPeerMessage(int targetConnectionId, int id, String address, int port) {
        PeerReference message = new PeerReference(id, address, port);

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
