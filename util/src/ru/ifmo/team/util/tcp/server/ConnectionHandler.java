package ru.ifmo.team.util.tcp.server;

import ru.ifmo.team.util.ExceptionExpander;
import ru.ifmo.team.util.IMessageAcceptor;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;
import ru.ifmo.team.util.msgencoding.EncodingException;
import ru.ifmo.team.util.msgencoding.MessageStreamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
/*package*/ class ConnectionHandler extends Thread implements IConnectionHandler {

    public static final int POLLING_DELAY = 50;

    private final Socket socket;
    private final PrefixLogger logger;
    private final TCPServer server;
    private final String ip;

    private IMessageAcceptor messageAcceptor;
    private MessageStreamer msgStreamer;
    private boolean shutdown = false;
    private InputStream is;
    private OutputStream os;

    public ConnectionHandler(Socket socket, TCPServer server, Logger logger) {
        this.socket = socket;
        this.server = server;
        this.logger = new PrefixLogger("Connection_" + hashCode(), logger);
        this.ip = socket.getRemoteSocketAddress().toString();
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            msgStreamer = new MessageStreamer(is, os);
        } catch (IOException e) {
            this.logger.log("Stream mapping failed: " + e.getMessage());
            finish();
        }
    }

    public void run() {
        synchronized (ip) {
            if (messageAcceptor == null) {
                try {
                    ip.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        while (!shutdown) {
            try {
                String message = msgStreamer.receiveMsg();
                if (message != null) {
                    messageAcceptor.acceptMessage(ip, message);
                    logger.log("Message received (size: " + message.length() + "): " + message);
                } else {
                    Thread.sleep(POLLING_DELAY);
                }
            } catch (InterruptedException e) {
                logger.log("Message Streamer interrupted while waiting for a message");
            } catch (EncodingException e) {
                logger.log("Unable to receive message, closing connection: " + ExceptionExpander.expandException(e));
                shutdown = true;
            }
        }

        finish();
    }

    public void sendMessage(String message) throws ServerException {
        if (server.isStarted() && !shutdown) {
            try {
                logger.log("Sending message (size: " + message.length() + "): " + message);
                msgStreamer.sendMsg(message);
                logger.log("Message sent");
            } catch (EncodingException e) {
                String msg = "Unable to send message, closing connection";
                logger.log(msg + ": " + ExceptionExpander.expandException(e));
                throw new ServerException(msg, e);
            }
        } else {
            String msg = "Unable to send message: server is not started";
            logger.log(msg);
            throw new ServerException("Unable to send message: server is not started");
        }
    }

    public void registerMessageAcceptor(IMessageAcceptor messageAcceptor) {
        this.messageAcceptor = messageAcceptor;
        synchronized (ip) {
            ip.notify();
        }
    }

    private void finish() {
        logger.log("Stopping ConnectionHandler responder");
        try {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            socket.close();
        } catch (IOException e) {
            logger.log("Unable to close socket properly: " + e.getMessage());
        }
        messageAcceptor.connectionClosed(ip);
        server.unregisterConnection(ip);
        logger.log("ConnectionHandler stopped");
    }

    public void shutDown() {
        shutdown = true;
    }

}
