package ru.ifmo.team.util.msgencoding;

import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;

import java.io.*;
import java.net.SocketTimeoutException;

/**
 * User: Daniel Penkin
 * Date: Apr 26, 2009
 * Version: 1.0
 */
public class MessageStreamer {

    public static final int BUFFER_SIZE = 4096;
    public static final int POLLING_DELAY = 250;

    private final OutputStream os;
    private final PrefixLogger logger;
    private final BufferedInputStream input;

    public MessageStreamer(InputStream is, OutputStream os, Logger logger) {
        input = new BufferedInputStream(is, BUFFER_SIZE);
        this.os = os;
        this.logger = new PrefixLogger("MS", logger);
    }

    public void sendMsg(String message) throws EncodingException {
        if ((os == null) || (message == null)) {
            return;
        }
        byte[] msg = message.getBytes();
        int size = msg.length;
        synchronized (os) {
            try {
//                logger.log("Outgoing message, size: " + size);
                for (int i = 4; i > 0; i--) {
                    byte b = (byte) (size >>> 8 * (i - 1));
                    os.write(b);
                }
//                logger.log("Body: " + message);
                os.write(msg);
                os.flush();
//                logger.log("Message sent");
            } catch (IOException e) {
                throw new EncodingException("Sending message failed due to I/O error", e);
            }
        }
    }

    public boolean hasInbox() {
        try {
            return input.available() >= 4;
        } catch (IOException e) {
            return false;
        }
    }

    public String receiveMsg() throws EncodingException {
        if (input == null) {
            return null;
        }
        if (hasInbox()) {
            synchronized (input) {
                try {

                    byte[] header = new byte[4];
                    int read = input.read(header);

                    int size = 0;
                    for (int i = 0; i < 4; i++) {
                        byte b = header[i];
                        size = (size << 8) ^ (b & 0x000000FF);
                    }

//                    logger.log("Incoming message, size: " + size + " [ " + Arrays.toString(header) + " ]");

                    if (size <= 0) {
                        throw new EncodingException("Size of incoming message is negative or zero");
                    }
                    byte[] msg = new byte[size];
                    read = input.read(msg);
                    //TODO check if whole message was read
                    String message = new String(msg);

//                    logger.log("Body: " + message);

                    return message;

                } catch (SocketTimeoutException e) {
                    throw new EncodingException("Connection timeout");
                } catch (EOFException e) {
                    throw new EncodingException("Connection was closed on client side");
                } catch (IOException e) {
                    throw new EncodingException("Receiving message failed due to I/O error", e);
                }
            }
        } else {
            return null;
        }
    }

//    private int readNBytes(byte[] dest, int n) throws IOException {
//        int offset = 0;
//        int left = n;
//        int read = -1;
//        boolean finish = false;
//        while (!finish) {
//            read = is.read(dest, offset, left);
//            if (read < 0) {
//                break;
//            }
//            offset += read;
//            if (read == left) {
//                finish = true;
//            }
//            left -= read;
//        }
//        return n - left;
//    }
}
