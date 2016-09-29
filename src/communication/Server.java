package communication;

import Interfaces.ICommunicationListener;
import Util.Constants;
import Util.MessageParser;
import data.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public class Server {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private Socket connectedSocket;
    private ServerSocket serverSocket;
    
    private ListenRunnable listenRunnable;
    private SendRunnable sendRunnable;

    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());

    public Server(int port) {

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        listeners.stream().forEach((sl) -> {
            sl.OnServerStarted();
        });
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

    public void StayOpenForConnection() {        
        if (connectedSocket == null || !connectedSocket.isConnected()) {
            ListenForConnection();
        }
    }

    private void ListenForConnection() {
        try {
            LOGGER.log(Level.INFO, "Waiting for client");

            connectedSocket = serverSocket.accept();

            LOGGER.log(Level.INFO, "Accepted connection {0}", serverSocket.getInetAddress().toString());

            listenRunnable = new ListenRunnable("SERVER", new BufferedReader(new InputStreamReader(connectedSocket.getInputStream())));
            sendRunnable = new SendRunnable("SERVER", new PrintWriter(connectedSocket.getOutputStream(), true));

            listenRunnable.UpdateListeners(listeners);
            sendRunnable.UpdateListeners(listeners);

            Thread listenThread = new Thread(listenRunnable);
            Thread sendThread = new Thread(sendRunnable);

            listenThread.start();
            sendThread.start();

            listeners.stream().forEach((sl) -> {
                sl.OnServerAcceptedConnection();
            });

        } catch (IOException ex) {
            listeners.stream().forEach((sl) -> {
                sl.OnServerError();
            });
            StopConnection();
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Shuts down the listeners and itself.
     */
    public void StopConnection() {
        LOGGER.log(Level.INFO, "Shutting down server.");

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
    }

    public void Stop() {
        StopConnection();
    }

    public boolean RunnablesHaveStopped() {
        return listenRunnable == null || sendRunnable == null || !listenRunnable.isRunning() || !sendRunnable.isRunning();
    }
    
    public boolean HasConnection()
    {
        return connectedSocket != null && connectedSocket.isConnected();
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

        if (sendRunnable == null) {
            LOGGER.log(Level.SEVERE, "Sendrunnable is null for msg {0}", message);
            return;
        }

        sendRunnable.writeMessage(jsonObj.toString());
    }

    public void writeMessage(Message message) {

        JSONObject jsonObj = new JSONObject(message);

        if (sendRunnable == null) {
            LOGGER.log(Level.SEVERE, "Sendrunnable is null for msg {0}", message.getMsg());
            return;
        }

        sendRunnable.writeMessage(jsonObj.toString());
    }

    public String getAddress() {
        return Integer.toString(serverSocket.getLocalPort());
    }

}
