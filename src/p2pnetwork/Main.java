package p2pnetwork;

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

        byte id = 1;
        /*
        if (args.length < 1) {
            LOGGER.log(Level.WARNING, "Usage: java -jar P2PNetwork { ID }");
            return;
        }

        byte id = -1;

        try {
            id = Byte.parseByte(args[0]);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Usage: java -jar P2PNetwork { ID }");
            return;
        }*/

        LOGGER.log(Level.INFO, "Starting up P2P application...");

        Peer controller = new Peer(id);

        controller.Start();
    }

}
