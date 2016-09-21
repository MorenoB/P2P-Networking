package communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener thread.
 */
class ListenRunnable implements Runnable {

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
        this.running = true;

        try {
            while ((inputLine = in.readLine()) != null) {
                
                if (!running) {
                    break;
                }
                
                LOGGER.log(Level.INFO, name + " Recieved {0}", inputLine);
                
                if(inputLine.equals("quit"))
                {
                    stop();
                }
                
                // Write to queue
                queue.add(inputLine);
            }
        } catch (IOException e) {
            
             LOGGER.log(Level.SEVERE, name, e);
        }

        running = false;
    }

    public void stop() {
        LOGGER.log(Level.INFO, "{0} stopping Listen Runnable...", name);
        running = false;
    }

    public String getMessage() {
        return queue.poll();
    }

    public boolean isRunning() {
        return running;
    }
}
