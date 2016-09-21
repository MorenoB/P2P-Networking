package p2pnetwork;

import Util.ApplicationSettings;
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
    
    private Server server;
    private Client client;

    public PeerController() {
        isRunning = true;
    }
    
    public void Start()
    {
        server = new Server();
        
        client = new Client();
        
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
        
        client.SetupConnection("localhost", ApplicationSettings.SERVERPORT);
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
    
    private void Stop()
    {
        client.Stop();
        server.Stop();
        
        isRunning = false;
    }
    
}
