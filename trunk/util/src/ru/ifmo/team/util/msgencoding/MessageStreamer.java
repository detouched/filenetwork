package ru.ifmo.team.util.msgencoding;

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
    private final BufferedInputStream input;

    public MessageStreamer(InputStream is, OutputStream os) {
        input = new BufferedInputStream(is, BUFFER_SIZE);
        this.os = os;
    }

    public void sendMsg(String message) throws EncodingException {
        if ((os == null) || (message == null)) {
            return;
        }
        byte[] msg = message.getBytes();
        int size = msg.length;
        synchronized (os) {
            try {
                for (int i = 4; i > 0; i--) {
                    byte b = (byte) (size >>> 8 * (i - 1));
                    os.write(b);
                }
                os.write(msg);
                os.flush();
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
                    input.read(header);

                    int size = 0;
                    for (int i = 0; i < 4; i++) {
                        byte b = header[i];
                        size = (size << 8) ^ (b & 0x000000FF);
                    }

                    if (size <= 0) {
                        throw new EncodingException("Size of incoming message is negative or zero");
                    }
                    byte[] msg = new byte[size];
                    int offset = 0;
                    while (offset < size) {
                        int read = input.read(msg, offset, size - offset);
                        offset += read;
                    }
                    return new String(msg);
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

}
