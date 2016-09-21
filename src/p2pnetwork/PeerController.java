package p2pnetwork;

import Interfaces.ICommunicationListener;
import Util.ApplicationSettings;
import Util.MessageParser;
import communication.Server;
import communication.Client;
import java.util.logging.Level;
import java.util.logging.Logger;
import communication.messages.Message;

/**
 *
 * @author Moreno
 */
public class PeerController implements ICommunicationListener {

    private static final Logger LOGGER = Logger.getLogger(PeerController.class.getCanonicalName());

    public boolean isRunning;

    private Server server;
    private Client client;

    public PeerController() {
        isRunning = true;
    }

    public void Start() {
        server = new Server();

        client = new Client();

        server.AddListener(this);
        client.AddListener(this);

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

        client.writeMessage("Hello!");
        
        server.writeMessage("Also testing!");

        while (isRunning) {
            Update();
        }
    }

    private void Update() {

    }

    private void Stop() {
        client.Stop();
        server.Stop();

        isRunning = false;
    }

    @Override
    public void OnClientStarted() {
        LOGGER.log(Level.INFO, "Client has started!");
    }

    @Override
    public void OnClientConnectedToServer() {
        LOGGER.log(Level.INFO, "Client connected to a server!");
    }

    @Override
    public void OnClientDisconnected() {
        LOGGER.log(Level.INFO, "Client has disconnected!");
    }

    @Override
    public void OnClientError() {
        LOGGER.log(Level.SEVERE, "Client has an error!");
    }

    @Override
    public void OnClientSentMessage() {
        LOGGER.log(Level.INFO, "Client has sent a message!");
    }

    @Override
    public void OnClientRecievedMessage() {

        Message recievedMsg = MessageParser.DecodeJSON(client.getMessage());

        LOGGER.log(Level.INFO, "Client recieved message = {0}", recievedMsg.getMsg());
    }

    @Override
    public void OnServerStarted() {
        LOGGER.log(Level.INFO, "Server started!");
    }

    @Override
    public void OnServerAcceptedConnection() {
        LOGGER.log(Level.INFO, "Server has accepted incoming connection!");
    }

    @Override
    public void OnServerError() {
        LOGGER.log(Level.SEVERE, "Server has an error!");
    }

    @Override
    public void OnServerSentMessage() {
        LOGGER.log(Level.INFO, "Server has sent a message!");
    }

    @Override
    public void OnServerRecievedMessage() {

        Message recievedMsg = MessageParser.DecodeJSON(server.getMessage());

        LOGGER.log(Level.INFO, "Server recieved message = {0}", recievedMsg.getMsg());
    }

}
