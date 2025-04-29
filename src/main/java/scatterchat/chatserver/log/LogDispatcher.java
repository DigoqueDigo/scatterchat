package scatterchat.chatserver.log;

import java.util.Iterator;
import java.util.List;

import scatterchat.protocol.message.LogCausalMessage;
import scatterchat.protocol.message.Message;


public class LogDispatcher implements Runnable {

    public static final long DELAY = 0L;
    public static final long PERIOD = 500L;
    public static final long TOLERANCE = 500L;

    private Logger logger;
    private List<LogCausalMessage> logBuffer;


    public LogDispatcher(Logger logger, List<LogCausalMessage> logBuffer) {
        this.logger = logger;
        this.logBuffer = logBuffer;
    }


    @Override
    public void run() {

        synchronized (this.logBuffer) {

            this.logBuffer.sort(null);
            Iterator<LogCausalMessage> iterator = this.logBuffer.iterator();

            System.out.println("[LogOrganizer] start");
            System.out.println("[LogOrganizer] logs: " + this.logBuffer);

            while (iterator.hasNext()) {

                LogCausalMessage logCausalMessage = iterator.next();
                long timestamp = logCausalMessage.getTimestamp();
                long currentTimestamp = System.currentTimeMillis();

                if (currentTimestamp - timestamp > LogDispatcher.TOLERANCE) {
                    Message message = logCausalMessage.getCausalMessage().getMessage();
                    this.logger.write(message);
                    iterator.remove();
                    System.out.println("[LogOrganizer] add log: " + message);
                }
            }
        }
    }
}