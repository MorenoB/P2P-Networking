package communication;

import Util.ApplicationSettings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Moreno
 */
public class Client implements Runnable {

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
                Thread.sleep(ApplicationSettings.CYCLEWAIT);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void SetupConnection(String host, int port) {
        if (hasConnection) {
            return;
        }

        try {
            connectedSocket = new Socket(host, port);
            hasConnection = connectedSocket.isConnected();

            LOGGER.log(Level.INFO, "Succesfully connected to {0}", connectedSocket.getInetAddress().toString());

            listenRunnable = new ListenRunnable("CLIENT", new BufferedReader(new InputStreamReader(connectedSocket.getInputStream())));
            sendRunnable = new SendRunnable("CLIENT", new PrintWriter(connectedSocket.getOutputStream(), true));

            Thread listenThread = new Thread(listenRunnable);
            Thread sendThread = new Thread(sendRunnable);

            listenThread.start();
            sendThread.start();

        } catch (IOException ex) {

            hasConnection = false;
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

        hasConnection = false;
    }
    
    private boolean RunnablesHaveStopped()
    {
        //Considered Stopped if we have a connected socket and we have got a non working runnable
        return ((connectedSocket != null && connectedSocket.isConnected()) &&
                (!listenRunnable.isRunning() || !sendRunnable.isRunning()));
    }

    public void Stop() {
        StopConnection();
        running = false;
    }

    /**
     * Fetches the latest message from the listener.
     *
     * @return XML message.
     */
    public String getMessage() {
        return listenRunnable.getMessage();
    }

    /**
     * Writes a message to the sender buffer.
     *
     * @param message XML message.
     */
    public void writeMessage(String message) {
        
        //User has no connection
        /*if (!hasConnection) {
            LOGGER.log(Level.INFO, "Client has no connection, setting up connection...");
            SetupConnection(host, port);
        }

        //User has connection to wrong host.
        if (hasConnection && !connectedSocket.getInetAddress().getHostAddress().equals(host)) {
            try {
                LOGGER.log(Level.INFO, "Client connected to wrong host, re setting up correct connection...");
                connectedSocket.close();
                SetupConnection(host, port);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }*/

        sendRunnable.writeMessage(message);
    }

    public boolean isRunning() {
        return running;
    }
}
