package communication;

import Util.ApplicationSettings;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sender thread.
 */
class SendRunnable implements Runnable {

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
                    Thread.sleep(ApplicationSettings.CYCLEWAIT);
                } catch (Throwable e) {
                    LOGGER.log(Level.SEVERE, name, e);
                }
            } else {
                // Send outputLine to client
                out.println(outputLine);

                LOGGER.log(Level.INFO, name + " sent {0}", outputLine);
            }
        }
    }

    public void stop() {
        LOGGER.log(Level.INFO, "{0} stopping Send Runnable...", name);
        running = false;
    }

    public void writeMessage(String message) {
        queue.add(message);
    }
    
    public boolean isRunning()
    {
        return running;
    }
}
