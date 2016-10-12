package communication;

import Util.Constants;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
import Util.MessageParser;
import data.Message;
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public class Client implements Runnable {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private boolean hasConnection;
    private boolean running;

    private ListenRunnable listenRunnable;
    private SendRunnable sendRunnable;
    private Socket connectedSocket;

    private static final Logger LOGGER = Logger.getLogger(Client.class.getCanonicalName());

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Starting up client...");

        running = true;

        while (running) {

            /*if(RunnablesHaveStopped())
            {
                LOGGER.log(Level.SEVERE, "ListenRunnable/SendRunnable stopped running! Shutting down connection...");
                StopConnection();
            }*/
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void AddListener(ICommunicationListener toAdd) {
        listeners.add(toAdd);

        if (listenRunnable != null) {
            listenRunnable.AddListener(toAdd);
        }

        if (sendRunnable != null) {
            sendRunnable.AddListener(toAdd);
        }
    }

    public boolean HasConnection() {
        return hasConnection;
    }

    public boolean SetupConnection(String host, int port) {
        if (hasConnection) {
            StopConnection();
        }

        try {
            connectedSocket = new Socket(host, port);
            hasConnection = connectedSocket.isConnected();

            listenRunnable = new ListenRunnable("CLIENT", connectedSocket);
            sendRunnable = new SendRunnable("CLIENT", connectedSocket);

            listenRunnable.UpdateListeners(listeners);
            sendRunnable.UpdateListeners(listeners);

            Thread listenThread = new Thread(listenRunnable, "Client-ListenRunnable");
            Thread sendThread = new Thread(sendRunnable, "Client-SendRunnable");

            listenThread.start();
            sendThread.start();
            
            listeners.stream().forEach((sl) -> {
                sl.OnClientConnectedToServer();
            });

            LOGGER.log(Level.INFO, "Succesfully connected to {0}", connectedSocket.getInetAddress().toString());
            
            return true;
        } catch (IOException ex) {

            listeners.stream().forEach((sl) -> {
                sl.OnClientError();
            });

            hasConnection = false;
            LOGGER.log(Level.SEVERE, "Client failed to connect to server!");
            
            return false;
        }

    }

    /**
     * Shuts down the listeners and itself.
     */
    public void StopConnection() {
        LOGGER.log(Level.INFO, "Shutting down client.");

        listenRunnable.Stop();
      
        sendRunnable.Stop();
       
        try {
            connectedSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
        listeners.stream().forEach((sl) -> {
            sl.OnClientDisconnected();
        });
        hasConnection = false;
    }

    private boolean RunnablesHaveStopped() {
        return listenRunnable == null || sendRunnable == null || !listenRunnable.isRunning() || !sendRunnable.isRunning();
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    public IMessage getMessage() {
        return MessageParser.DecodeJSON(listenRunnable.getRawMessage());
    }

    public boolean isReady() {
        return isRunning() && !RunnablesHaveStopped();
    }

    /**
     * Writes a message to the sender buffer.
     *
     * @param message XML message.
     */
    public void writeMessage(String message) {

        Message msgMessage = new Message(Constants.MSG_MESSAGE);
        msgMessage.setMsg(message);

        JSONObject jsonObj = new JSONObject(msgMessage);

        sendRunnable.writeMessage(jsonObj);
    }

    public void writeMessage(IMessage message) {

        JSONObject jsonObj = new JSONObject(message);

        sendRunnable.writeMessage(jsonObj);
    }

    public boolean isRunning() {
        return running;
    }
}
