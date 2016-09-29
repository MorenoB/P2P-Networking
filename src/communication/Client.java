package communication;

import Util.Constants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import Interfaces.ICommunicationListener;
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

    public void SetupConnection(String host, int port) {
        if (hasConnection) {
            return;
        }

        try {
            connectedSocket = new Socket(host, port);
            hasConnection = connectedSocket.isConnected();

            listeners.stream().forEach((sl) -> {
                sl.OnClientConnectedToServer();
            });

            LOGGER.log(Level.INFO, "Succesfully connected to {0}", connectedSocket.getInetAddress().toString());

            listenRunnable = new ListenRunnable("CLIENT", new BufferedReader(new InputStreamReader(connectedSocket.getInputStream())));
            sendRunnable = new SendRunnable("CLIENT", new PrintWriter(connectedSocket.getOutputStream(), true));

            listenRunnable.UpdateListeners(listeners);
            sendRunnable.UpdateListeners(listeners);

            Thread listenThread = new Thread(listenRunnable);
            Thread sendThread = new Thread(sendRunnable);

            listenThread.start();
            sendThread.start();

        } catch (IOException ex) {

            listeners.stream().forEach((sl) -> {
                sl.OnClientError();
            });

            hasConnection = false;
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Shuts down the listeners and itself.
     */
    public void StopConnection() {
        LOGGER.log(Level.INFO, "Shutting down client.");

        try {
            listenRunnable.Stop();
        } catch (Throwable e) {
        }
        try {
            sendRunnable.Stop();
        } catch (Throwable e) {
        }
        try {
            connectedSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
        listeners.stream().forEach((sl) -> {
            sl.OnClientDisconnected();
        });
        hasConnection = false;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean RunnablesHaveStopped() {
        return listenRunnable == null || sendRunnable == null || !listenRunnable.isRunning() || !sendRunnable.isRunning();
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    public Message getMessage() {
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

    public void writeMessage(Message message) {

        JSONObject jsonObj = new JSONObject(message);

        sendRunnable.writeMessage(jsonObj);
    }

    public boolean isRunning() {
        return running;
    }
}
