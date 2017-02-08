package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moren
 */
public class InfoMessage extends MessageObject implements IMessage{
    
    private int sourceId;
    
    public InfoMessage(String msg) {
        super(Constants.MSG_MESSAGE);
        
        this.msg = msg;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getSourceId() {
        return sourceId;
    }
}
