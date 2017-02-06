package communication;

import Interfaces.ICommunicationListener;
import Interfaces.IMessage;
import Util.Constants;
import Util.MessageParser;
import data.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Moreno
 */
public class Server implements Runnable {

    private final List<ICommunicationListener> listeners = new ArrayList<>();

    private ServerSocket serverSocket;

    private boolean running;

    private final int port;

    private final List<ListenRunnable> listenRunnables;

    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());

    public Server(int port) {

        listenRunnables = new ArrayList<>();
        this.port = port;
    }

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Starting up server...");

        running = true;

        try {
            serverSocket = new ServerSocket(port);

            listeners.stream().forEach((sl) -> {
                sl.OnServerStarted();
            });
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        while (running) {

            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            ListenForConnection();

        }
    }

    public void AddListener(ICommunicationListener toAdd) {
        listeners.add(toAdd);

        for (int i = 0; i < listenRunnables.size(); i++) {
            listenRunnables.get(i).AddListener(toAdd);
        }
    }

    private void ListenForConnection() {
        try {

            Socket connectedSocket = serverSocket.accept();

            //LOGGER.log(Level.INFO, "Accepted connection {0} at server connection " + serverSocket.getInetAddress().getHostName(), connectedSocket.getInetAddress().getHostAddress());

            ListenRunnable listenR = new ListenRunnable("SERVER", connectedSocket);
            
            listenR.UpdateListeners(listeners);

            listenRunnables.add(listenR);

            Thread listenThread = new Thread(listenR, "Server-ListenRunnable" + port);

            listenThread.start();

            listeners.stream().forEach((sl) -> {
                sl.OnServerAcceptedConnection();
            });

        } catch (IOException ex) {
            listeners.stream().forEach((sl) -> {
                sl.OnServerError(-1);
            });
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private ListenRunnable getListenRunnable(int port) {
        for (int i = 0; i < listenRunnables.size(); i++) {
            if (listenRunnables.get(i).getPort() == port) {
                return listenRunnables.get(i);
            }

        }

        return null;
    }

    /**
     * Shuts down the listeners.
     *
     * @param portNr Port number
     */
    public void StopConnection(int portNr) {
        LOGGER.log(Level.INFO, "Shutting server connection for port {0}", portNr);

        ListenRunnable listR = getListenRunnable(portNr);
        
        if(listR != null)
        {
            listR.Stop();

            listenRunnables.remove(listR);
        }
    }

    public void Stop() {
        //StopConnection();
        running = false;
    }

    public IMessage getMessage() {

        for (int i = 0; i < listenRunnables.size(); i++) {
            ListenRunnable listenR = listenRunnables.get(i);
            if (listenR == null) {
                continue;
            }

            if (!listenR.hasMessage()) {
                continue;
            }

            return MessageParser.DecodeJSON(listenR.getRawMessage());

        }

        return new Message("NULL");
    }

    public boolean isRunning() {
        return running;
    }

    public String getAddress() {
        return serverSocket.getInetAddress().getHostName();
    }

}
