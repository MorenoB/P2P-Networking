package communication;

import Interfaces.ICommunicationListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener thread.
 */
class ListenRunnable implements Runnable {

    private List<ICommunicationListener> listeners = new ArrayList<>();

    private BufferedReader in;
    private final Socket socket;
    private final ConcurrentLinkedQueue<String> queue;
    private boolean running;
    private final String name;

    private static final Logger LOGGER = Logger.getLogger(ListenRunnable.class.getCanonicalName());

    public ListenRunnable(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
        
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        String inputLine;

        running = true;

        try {
            while (running) {

                if ((inputLine = in.readLine()) == null) {
                    continue;
                }

                //LOGGER.log(Level.INFO, name + " Recieved {0}", inputLine);
                // Write to queue
                queue.add(inputLine);

                if ("SERVER".equals(name)) {
                    listeners.stream().forEach((listener) -> {
                        listener.OnServerRecievedMessage();
                    });
                } else {
                    listeners.stream().forEach((listener) -> {
                        listener.OnClientRecievedMessage();
                    });
                }
            }
        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, name, e);

            listeners.stream().forEach((listener) -> {
                listener.OnClientError();
            });
        }

        running = false;
    }
    
    public Socket getSocket()
    {
        return socket; 
    }
    
    public int getPort()
    {
        return socket.getPort();
    }

    public void UpdateListeners(List<ICommunicationListener> newListeners) {
        listeners = newListeners;
    }

    public void AddListener(ICommunicationListener listener) {
        
        if(!listeners.contains(listener))
            listeners.add(listener);
    }

    public void Stop() {
        LOGGER.log(Level.INFO, "{0} stopping Listen Runnable...", name);

        try {
            in.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        running = false;

        Thread.currentThread().stop();
    }
    
    public boolean hasMessage()
    {
        return !queue.isEmpty();
    }

    public String getRawMessage() {
        return queue.poll();
    }

    public boolean isRunning() {
        return running;
    }
}
