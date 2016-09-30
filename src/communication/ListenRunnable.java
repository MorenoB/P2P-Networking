package communication;

import Interfaces.ICommunicationListener;
import java.io.BufferedReader;
import java.io.IOException;
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

    private final BufferedReader in;
    private final ConcurrentLinkedQueue<String> queue;
    private boolean running;
    private final String name;

    private static final Logger LOGGER = Logger.getLogger(ListenRunnable.class.getCanonicalName());

    public ListenRunnable(String name, BufferedReader in) {
        this.name = name;
        this.in = in;
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

                LOGGER.log(Level.INFO, name + " Recieved {0}", inputLine);

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
        } catch (IOException e) {

            if (running) {
                LOGGER.log(Level.SEVERE, name, e);
            }
        }
        running = false;
    }

    public void UpdateListeners(List<ICommunicationListener> newListeners) {
        listeners = newListeners;
    }

    public void AddListener(ICommunicationListener listener) {
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

    public String getRawMessage() {
        return queue.poll();
    }

    public boolean isRunning() {
        return running;
    }
}
