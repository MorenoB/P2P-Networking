/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pnetwork;

import Util.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

/**
 *
 * @author Moren
 */
public class VisualizerRunnable implements Runnable{
    
    private boolean running = true;
    
    private List<Peer> peerList = new ArrayList();
    
    private JLabel lastRecievedMessageLabel;
    
    private Peer selectedPeer;
    
    public void Init(JLabel lastRecievedMessageLabel)
    {
        this.lastRecievedMessageLabel = lastRecievedMessageLabel;
    }
    
    public void UpdatePeerList(List<Peer> peers)
    {
        peerList = peers;
    }
    
    public void SelectNewPeer(int peerId)
    {
        for (int i = 0; i < peerList.size(); i++) {
            if(peerList.get(i).getId() == peerId)
                selectedPeer = peerList.get(i);
        }
    }
    
    private void UpdateLabels()
    {
        if(selectedPeer == null || selectedPeer.getLastRecievedMessage() == null) return;
        
        String txtToShow = "Last Msg: " + selectedPeer.getLastRecievedMessage().getMsg();
        lastRecievedMessageLabel.setText(txtToShow);
    }
    @Override
    public void run() {
        
        while(running)
        {
            UpdateLabels();
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                Logger.getLogger(VisualizerRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
}
