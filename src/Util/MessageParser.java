package Util;

import data.Message;
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public final class MessageParser {

    public static Message DecodeJSON(String rawJson) {
        
        JSONObject jsonObj = new JSONObject(rawJson);
        
        String msg = jsonObj.getString("msg");
        int messageType = jsonObj.getInt("messageType");
        int messageId = jsonObj.getInt("id");

        Message message = new Message(messageType, messageId);
        message.setMsg(msg);

        return message;
    }
    
    public static Message CreatePeerIDMessage(int peerId)
    {
        Message message = new Message(Constants.MSG_PEERID);
        
        message.setMsg(Integer.toString(peerId));
        
        return message;
    }
    
    public static Message CreatePeerIDRequest()
    {
        Message message = new Message(Constants.MSG_REQUEST_PEERID);
        
        message.setMsg("IHRE PEER ID BITTE!");
        
        return message;
    }
}
