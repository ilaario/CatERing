package catering.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Utility class to provide consistent logging across the application
 * with C-like formatting: chunked lines, prefix/dashes, and PID.
 */
public class LogManager {
    private static final int MAX_CONTENT_WIDTH = 80;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy - HH:mm.SSS");
    private static LogManager instance = null;

    private LogManager() {
        Logger rootLogger = Logger.getLogger("");
        // Remove existing handlers
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        // Add console handler with custom C-like formatter
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new CFormatter());
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }

    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public static Logger getLogger(Class<?> clazz) {
        getInstance(); // Ensure configured
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Custom formatter implementing C-style chunking, prefix and dashes.
     */
    private static class CFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String message = formatMessage(record);
            String timestamp = DATE_FORMAT.format(new Date(record.getMillis()));
            long pid = getProcessId();
            String level = mapLevel(record.getLevel());
            String prefix = String.format("[%s] - [PID:%d] - [%s] --> ", level, pid, timestamp);
            StringBuilder output = new StringBuilder();
            // Prepare dash line
            String dashes = new String(new char[prefix.length()]).replace('\0', '-');

            int idx = 0;
            int chunkNo = 0;
            int len = message.length();
            while (idx < len) {
                int scanLen = Math.min(MAX_CONTENT_WIDTH, len - idx);
                int newlinePos = message.indexOf('\n', idx);
                int chunkLen;
                boolean hasNewline = false;
                if (newlinePos >= 0 && newlinePos - idx < scanLen) {
                    chunkLen = newlinePos - idx;
                    hasNewline = true;
                } else {
                    chunkLen = scanLen;
                }
                String chunk = message.substring(idx, idx + chunkLen);
                if (chunkNo == 0) {
                    output.append(prefix).append("[").append(chunk).append("]\n");
                } else {
                    output.append(dashes).append("] --> [").append(chunk).append("]");
                    output.append("\n");
                }
                idx += chunkLen;
                if (hasNewline) {
                    idx++;
                }
                chunkNo++;
            }
            if (record.getThrown() != null) {
                output.append(formatException(record.getThrown()));
            }
            return output.toString();
        }

        private String formatException(Throwable throwable) {
            StringBuilder sb = new StringBuilder();
            sb.append(throwable.toString()).append("\n");
            for (StackTraceElement element : throwable.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
            return sb.toString();
        }

        private String mapLevel(Level lvl) {
            if (lvl == Level.FINEST || lvl == Level.FINER) return "TRACE";
            if (lvl == Level.FINE) return "DEBUG";
            if (lvl == Level.SEVERE) return "SYSER";
            if (lvl == Level.WARNING) return "APPER";
            if (lvl == Level.INFO) return "SYSIN"; // or APPIN based on context
            return lvl.getName();
        }

        private long getProcessId() {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            try {
                return Long.parseLong(jvmName.split("@")[0]);
            } catch (Exception e) {
                return -1;
            }
        }
    }
}