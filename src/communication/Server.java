package communication;

import Interfaces.ICommunicationListener;
import Util.Constants;
import Util.MessageParser;
import communication.messages.Message;
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
public class Server implements Runnable {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private Socket connectedSocket;

    private boolean running;
    private boolean isWaitingForConnection;

    private ListenRunnable listenRunnable;
    private SendRunnable sendRunnable;

    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Starting up server...");

        running = true;

        while (running) {
            if (!isWaitingForConnection) {
                ListenForConnection();
            }

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

    private void ListenForConnection() {
        try {
            isWaitingForConnection = true;

            ServerSocket server = new ServerSocket(Constants.SERVERPORT);

            listeners.stream().forEach((sl) -> {
                sl.OnServerStarted();
            });
            LOGGER.log(Level.INFO, "Waiting for client");

            connectedSocket = server.accept();

            LOGGER.log(Level.INFO, "Accepted connection {0}", server.getInetAddress().toString());

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

        isWaitingForConnection = false;
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    private boolean RunnablesHaveStopped() {
        //Considered Stopped if we have a connected socket and we have got a non working runnable
        return ((connectedSocket != null && connectedSocket.isConnected())
                && (!listenRunnable.isRunning() || !sendRunnable.isRunning()));
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

    public boolean isReady() {
        return isRunning() && !RunnablesHaveStopped();
    }

    public void writeMessage(Message message) {

        JSONObject jsonObj = new JSONObject(message);

        if (sendRunnable == null) {
            LOGGER.log(Level.SEVERE, "Sendrunnable is null for msg {0}", message.getMsg());
            return;
        }

        sendRunnable.writeMessage(jsonObj.toString());
    }

    public boolean isRunning() {
        return running;
    }

}
