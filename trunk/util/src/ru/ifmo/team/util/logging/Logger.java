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

    /*package*/ void log(String message) {
        try {
            if (message.length() > 500) {
                message = message.substring(0, 200) + " *** message too big, partially omitted *** " +
                        message.substring(message.length() - 200);
            }

            String msg = message.replace("\n\r", "");
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