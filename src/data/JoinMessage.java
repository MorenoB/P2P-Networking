/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import Interfaces.IMessage;
import Util.Constants;

/**
 *
 * @author Moreno
 */
public class JoinMessage extends MessageObject implements IMessage{

    private final PeerReference joiningPeer;
    
    public JoinMessage(PeerReference joiningPeer) {
        super(Constants.MSG_JOIN);
        
        this.msg = "Joining request.";
        
        this.joiningPeer = joiningPeer;
    }

    public PeerReference getJoiningPeer() {
        return joiningPeer;
    }
}
