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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

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
    private final List<SendRunnable> sendRunnables;

    private final ConcurrentLinkedQueue<Integer> portQueue;

    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());

    public Server(int port) {

        listenRunnables = new ArrayList<>();
        sendRunnables = new ArrayList<>();
        this.port = port;

        portQueue = new ConcurrentLinkedQueue<>();
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

        for (int i = 0; i < sendRunnables.size(); i++) {
            sendRunnables.get(i).AddListener(toAdd);
        }
    }

    private void ListenForConnection() {
        try {

            Socket connectedSocket = serverSocket.accept();

            //LOGGER.log(Level.INFO, "Accepted connection {0} at server connection " + serverSocket.getInetAddress().getHostName(), connectedSocket.getInetAddress().getHostAddress());
            portQueue.add(connectedSocket.getPort());

            ListenRunnable listenR = new ListenRunnable("SERVER", connectedSocket);
            SendRunnable sendR = new SendRunnable("SERVER", connectedSocket);

            listenR.UpdateListeners(listeners);
            sendR.UpdateListeners(listeners);

            listenRunnables.add(listenR);
            sendRunnables.add(sendR);

            Thread listenThread = new Thread(listenR, "Server-ListenRunnable");
            Thread sendThread = new Thread(sendR, "Server-SendRunnable");

            listenThread.start();
            sendThread.start();

            listeners.stream().forEach((sl) -> {
                sl.OnServerAcceptedConnection();
            });

        } catch (IOException ex) {
            listeners.stream().forEach((sl) -> {
                sl.OnServerError();
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

    private SendRunnable getSendRunnable(int port) {
        for (int i = 0; i < sendRunnables.size(); i++) {
            if (sendRunnables.get(i).getPort() == port) {
                return sendRunnables.get(i);
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
        SendRunnable sendR = getSendRunnable(portNr);

        try {
            listR.Stop();

            /*while (listR.isRunning()) {
                //
            }*/

            listenRunnables.remove(listR);
        } catch (Throwable e) {
        }
        try {
            sendR.Stop();

            /*while (sendR.isRunning()) {
                //
            }*/

            sendRunnables.remove(sendR);
        } catch (Throwable e) {
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

    public void writeMessage(IMessage message) {

        if (portQueue.peek() == null) {
            LOGGER.log(Level.SEVERE, "PortQueue is empty, unable to send message {0}", message.getMsg());
            return;
        }
        int portNr = portQueue.poll();

        JSONObject jsonObj = new JSONObject(message);

        SendRunnable sendR = getSendRunnable(portNr);

        if (sendR == null) {
            LOGGER.log(Level.SEVERE, "Sendrunnable is null for msg {0}", message.getMsg());
            return;
        }

        sendR.writeMessage(jsonObj);
    }

    public boolean isRunning() {
        return running;
    }

    public String getAddress() {
        return serverSocket.getInetAddress().getHostName();
    }

}
