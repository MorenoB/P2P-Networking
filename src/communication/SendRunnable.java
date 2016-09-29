package communication;

import Interfaces.ICommunicationListener;
import Util.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 * Sender thread.
 */
class SendRunnable implements Runnable {

    private List<ICommunicationListener> listeners = new ArrayList<>();
    private final PrintWriter out;
    private final ConcurrentLinkedQueue<JSONObject> queue;
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
        running = true;

        while (running) {
            JSONObject lastObj = queue.poll();
            if (lastObj == null) {
                try {
                    Thread.sleep(Constants.CYCLEWAIT);
                } catch (Throwable e) {
                    LOGGER.log(Level.SEVERE, name, e);
                }
            } else {
                // Send outputLine
                out.println(lastObj);
                
                out.flush();

                if ("SERVER".equals(name)) {
                    listeners.stream().forEach((listener) -> {
                        listener.OnServerSentMessage(lastObj);
                    });
                } else {
                    listeners.stream().forEach((listener) -> {
                        listener.OnClientSentMessage(lastObj);
                    });
                }
            }
        }
    }

    public void UpdateListeners(List<ICommunicationListener> newListeners) {
        listeners = newListeners;
    }

    public void AddListener(ICommunicationListener listener) {
        listeners.add(listener);
    }

    public void Stop() {
        LOGGER.log(Level.INFO, "{0} stopping Send Runnable...", name);
        
        out.close();
        
        running = false;
    }

    public void writeMessage(JSONObject message) {
        queue.add(message);
    }

    public boolean isRunning() {
        return running;
    }
}
