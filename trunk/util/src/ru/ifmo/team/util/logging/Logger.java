package ru.ifmo.team.util.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * User: Daniel Penkin
 * Date: Apr 27, 2009
 * Version: 1.0
 */
public class Logger {

    private static final String MONITOR = "";
    private File logFile;

    public Logger(String path) {
        logFile = new File(path);
    }

    public boolean clearLog() {
        return logFile.delete();
    }

    /*package*/ void log(String logMessage) {
        try {
            String msg = logMessage;
            if (msg.length() > 1000) {
                msg = msg.substring(0, 400) + " *** message too big, partially omitted *** " +
                        msg.substring(msg.length() - 400);
            }
            msg = msg.replace("\n\r", "");
            msg = msg.replace("\n", "");
            msg = msg.replace("\t", " ");
            synchronized (MONITOR) {
                FileWriter logWriter = new FileWriter(logFile, true);
                //TODO date
                Date now = new Date(System.currentTimeMillis());

                logWriter.write(now + " " + msg + "\n");
                logWriter.close();
            }
        } catch (IOException e) {
            System.out.println("Can't write log into file: " + e.getMessage());
        }
    }

}