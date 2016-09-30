package Util;

import Interfaces.IMessage;
import data.Message;
import data.PeerReference;
import data.Search;
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

        return message;
    }

    public static Message CreatePeerIDRequest() {
        Message message = new Message(Constants.MSG_REQUEST_PEERID);

        message.setMsg("IHRE PEER ID BITTE!");

        return message;
    }

    public static PeerReference CreateJoinPeerMessage(int id, String address, int port) {
        PeerReference message = new PeerReference(id, address, port);

        message.setMsg("Joining peer request.");

        return message;
    }
    
    public static Search CreateSearchPeerMessage(PeerReference sourcePeerRef, int id)
    {
        Search message = new Search(sourcePeerRef, id);
        
        return message;
    }
    
    public static Search CreateSearchPeerFoundMessage(PeerReference sourcePeerRef, PeerReference targetPeerRef)
    {
        Search message = new Search(sourcePeerRef, targetPeerRef);
        
        return message;
    }
}
