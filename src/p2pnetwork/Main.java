package p2pnetwork;

import Util.Constants;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Moreno
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getCanonicalName());

    /**
     * @param args the command line arguments
     */
    public static void Notmain(String[] args) {

        LOGGER.log(Level.INFO, "Starting up P2P application...");

        byte id = 0;
        Peer peer = new Peer(id, Constants.SERVERPORT);

        peer.setBootPeer(true);
        peer.Start();

        Thread peerThread = new Thread(peer);
        peerThread.start();

        //peer.JoinNetworkWithIP("localhost", Constants.SERVERPORT);
        //peer.ConnectToPeer("localhost");
    }

}
