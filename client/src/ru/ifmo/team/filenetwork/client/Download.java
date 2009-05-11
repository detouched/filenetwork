package ru.ifmo.team.filenetwork.client;

import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * User: Daniel Penkin
 * Date: May 10, 2009
 * Version: 1.0
 */
/*package*/ class Download {

    private final File file;
    private final String sid;
    private final FileClient client;
    private final PrefixLogger logger;

    private FileOutputStream output;
    private int partCounter = 1;
    private int partTotal;
    private boolean canWrite = true;

    Download(File file, String sid, FileClient client, Logger logger) {
        this.file = file;
        this.sid = sid;
        this.client = client;
        this.logger = new PrefixLogger("DWNLD_" + sid, logger);
        try {
            output = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            this.logger.log("Unable to write in file");
            canWrite = false;
        }
    }

    public boolean canWrite() {
        return canWrite;
    }

    public File getFile() {
        return file;
    }

    public void processPart(int number, int total, byte[] data) throws IOException {
        if (!canWrite) {
            logger.log("Unable to process part: can't write to file");
            return;
        }
        if (partCounter == number) {
            if (partCounter == 1) {
                partTotal = total;
            }
            output.write(data);
            partCounter++;
        }
        checkReadiness();
    }

    private void checkReadiness() throws IOException {
        if (partCounter == partTotal + 1) {
            output.flush();
            output.close();
            client.downloadCompleted(sid);
        }
    }
}
