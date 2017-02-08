package communication;

import Interfaces.ICommunicationListener;
import Util.Constants;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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
    private PrintWriter out;
    private final ConcurrentLinkedQueue<JSONObject> queue;
    private final Socket socket;
    private boolean running;
    private final String name;

    private static final Logger LOGGER = Logger.getLogger(SendRunnable.class.getCanonicalName());

    public SendRunnable(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
        this.queue = new ConcurrentLinkedQueue<>();

        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                Thread.sleep(Constants.CYCLEWAIT);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, name, e);
            }

            JSONObject lastObj = queue.poll();

            if (lastObj == null) {
                continue;
            } else {
                // Send outputLine
                out.println(lastObj);

                out.flush();

                if ("CLIENT".equals(name)) {
                    listeners.stream().forEach((listener) -> {
                        listener.OnClientSentMessage(lastObj);
                    });
                }
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public int getPort() {
        return socket.getPort();
    }

    public void UpdateListeners(List<ICommunicationListener> newListeners) {
        listeners = newListeners;
    }

    public void AddListener(ICommunicationListener listener) {
        listeners.add(listener);
    }

    public void Stop() {
        //LOGGER.log(Level.INFO, "{0} stopping Send Runnable...", name);

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
