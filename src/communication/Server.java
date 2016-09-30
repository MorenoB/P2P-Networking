package communication;

import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
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
public class Server implements Runnable {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private Socket connectedSocket;
    private ServerSocket serverSocket;

    private boolean running;

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

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Starting up server...");

        running = true;

        while (running) {
                         
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            if (connectedSocket == null ) {
                ListenForConnection();
            }

            /*if(RunnablesHaveStopped())
            {
                LOGGER.log(Level.SEVERE, "ListenRunnable/SendRunnable stopped running! Shutting down connection...");
                StopConnection();
            }*/
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
            LOGGER.log(Level.INFO, "Server waiting for client");

            connectedSocket = serverSocket.accept();

            LOGGER.log(Level.INFO, "Accepted connection {0}", serverSocket.getInetAddress().toString());

            listenRunnable = new ListenRunnable("SERVER", new BufferedReader(new InputStreamReader(connectedSocket.getInputStream())));
            sendRunnable = new SendRunnable("SERVER", new PrintWriter(connectedSocket.getOutputStream(), true));

            listenRunnable.UpdateListeners(listeners);
            sendRunnable.UpdateListeners(listeners);

            Thread listenThread = new Thread(listenRunnable, "Server-ListenRunnable");
            Thread sendThread = new Thread(sendRunnable, "Server-SendRunnable");

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
            listenRunnable.Stop();
        } catch (Throwable e) {
        }
        try {
            sendRunnable.Stop();
        } catch (Throwable e) {
        }
        try {
            serverSocket.close();
            
            if(connectedSocket != null)
                connectedSocket.close();
            
            connectedSocket = null;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    private boolean RunnablesHaveStopped() {
        return listenRunnable == null || sendRunnable == null || !listenRunnable.isRunning() || !sendRunnable.isRunning();
    }

    public IMessage getMessage() {
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

        sendRunnable.writeMessage(jsonObj);
    }

    public boolean isReady() {
        return isRunning() && !RunnablesHaveStopped();
    }

    public void writeMessage(IMessage message) {

        JSONObject jsonObj = new JSONObject(message);

        if (sendRunnable == null) {
            LOGGER.log(Level.SEVERE, "Sendrunnable is null for msg {0}", message.getMsg());
            return;
        }

        sendRunnable.writeMessage(jsonObj);
    }

    public boolean isRunning() {
        return running;
    }

    public String getAddress() {
        return serverSocket.getInetAddress().getHostName();
    }

}
