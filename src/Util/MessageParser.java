package Util;

import communication.messages.Message;
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
    
    public static Message CreatePeerIDMessage(byte peerId)
    {
        Message message = new Message(Constants.MSG_PEERID);
        
        message.setMsg(Byte.toString(peerId));
        
        return message;
    }
}
