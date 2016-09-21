package p2pnetwork;

import communication.Server;
import communication.Client;
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
    public static void main(String[] args) {
        
        LOGGER.log(Level.INFO, "Starting up application...");
        
        PeerController controller = new PeerController();
        
        
        controller.Start();
    }
    
}
