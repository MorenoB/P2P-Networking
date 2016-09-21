package communication;

import Interfaces.ICommunicationListener;
import Util.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sender thread.
 */
class SendRunnable implements Runnable {

    private List<ICommunicationListener> listeners = new ArrayList<>();
    private final PrintWriter out;
    private final ConcurrentLinkedQueue<String> queue;
    private boolean running;
    private final String name;

    private static final Logger LOGGER = Logger.getLogger(SendRunnable.class.getCanonicalName());

    public SendRunnable(String name, PrintWriter out) {
        this.name = name;
        this.out = out;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        String outputLine;
        running = true;

        while (running) {
            outputLine = queue.poll();
            if (outputLine == null) {
                try {
                    Thread.sleep(Constants.CYCLEWAIT);
                } catch (Throwable e) {
                    LOGGER.log(Level.SEVERE, name, e);
                }
            } else {
                // Send outputLine
                out.println(outputLine);

                if ("SERVER".equals(name)) {
                    listeners.stream().forEach((listener) -> {
                        listener.OnServerSentMessage();
                    });
                } else {
                    listeners.stream().forEach((listener) -> {
                        listener.OnClientSentMessage();
                    });
                }
                LOGGER.log(Level.INFO, name + " sent {0}", outputLine);
            }
        }
    }

    public void UpdateListeners(List<ICommunicationListener> newListeners) {
        listeners = newListeners;
    }

    public void AddListener(ICommunicationListener listener) {
        listeners.add(listener);
    }

    public void stop() {
        LOGGER.log(Level.INFO, "{0} stopping Send Runnable...", name);
        running = false;
    }

    public void writeMessage(String message) {
        queue.add(message);
    }

    public boolean isRunning() {
        return running;
    }
}
