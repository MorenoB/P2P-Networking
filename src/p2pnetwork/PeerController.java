package p2pnetwork;

import communication.Server;
import communication.Client;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Moreno
 */
public class PeerController {
    
    private static final Logger LOGGER = Logger.getLogger(PeerController.class.getCanonicalName());
    
    public boolean isRunning;

    public PeerController() {
        isRunning = true;
    }
    
    public void Start()
    {
        Server server = new Server();
        
        Client client = new Client();
        
        Thread serverThread = new Thread(server);
        Thread clientThread = new Thread(client);
        
        
        //Start Threads
        serverThread.start();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        clientThread.start();
        
        client.SetupConnection("localhost", 80);
        client.writeMessage("HELLO!");
        
        server.writeMessage("Oh hai!");
        
        while(isRunning)
        {
            Update();
        }
    }
    
    
    private void Update()
    {
        
    }
    
}
