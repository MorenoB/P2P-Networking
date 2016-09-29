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
public class Client {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private ListenRunnable listenRunnable;
    private SendRunnable sendRunnable;
    private Socket connectedSocket;

    private static final Logger LOGGER = Logger.getLogger(Client.class.getCanonicalName());

    public void AddListener(ICommunicationListener toAdd) {
        listeners.add(toAdd);

        if (listenRunnable != null) {
            listenRunnable.AddListener(toAdd);
        }

        if (sendRunnable != null) {
            sendRunnable.AddListener(toAdd);
        }
    }

    public void SetupConnection(String host, int port) {
        if (HasConnection()) {
            return;
        }

        try {
            connectedSocket = new Socket(host, port);

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

            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Shuts down the listeners and itself.
     */
    public void StopConnection() {
        LOGGER.log(Level.INFO, "Shutting down client.");

        try {
            listenRunnable.stop();
        } catch (Throwable e) {
        }
        try {
            sendRunnable.stop();
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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean RunnablesHaveStopped() {
        return listenRunnable == null || sendRunnable == null || !listenRunnable.isRunning() || !sendRunnable.isRunning();
    }
    
    public boolean HasConnection()
    {
        return connectedSocket != null && connectedSocket.isConnected();
    }

    public void Stop() {
        StopConnection();
    }

    public Message getMessage() {
        return MessageParser.DecodeJSON(listenRunnable.getRawMessage());
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

        sendRunnable.writeMessage(jsonObj.toString());

        listeners.stream().forEach((sl) -> {
            sl.OnClientSentMessage();
        });
    }

    public void writeMessage(Message message) {

        JSONObject jsonObj = new JSONObject(message);

        sendRunnable.writeMessage(jsonObj.toString());

        listeners.stream().forEach((sl) -> {
            sl.OnClientSentMessage();
        });
    }
}
