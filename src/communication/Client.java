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
import org.json.JSONObject;

/**
 *
 * @author Moreno
 */
public class Client implements Runnable {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private boolean hasConnection;
    private boolean running;

    private SendRunnable sendRunnable;
    private Socket connectedSocket;

    private static final Logger LOGGER = Logger.getLogger(Client.class.getCanonicalName());

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Starting up client...");

        running = true;

        while (running) {

            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void AddListener(ICommunicationListener toAdd) {
        listeners.add(toAdd);

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

            sendRunnable = new SendRunnable("CLIENT", connectedSocket);

            sendRunnable.UpdateListeners(listeners);

            Thread sendThread = new Thread(sendRunnable, "Client-SendRunnable");

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
        return sendRunnable == null || !sendRunnable.isRunning();
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    public boolean isReady() {
        return isRunning() && !RunnablesHaveStopped();
    }

    public void writeMessage(IMessage message) {

        JSONObject jsonObj = new JSONObject(message);

        sendRunnable.writeMessage(jsonObj);
    }

    public boolean isRunning() {
        return running;
    }
}
