package ru.ifmo.team.util.tcp.client;

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
import java.net.UnknownHostException;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
public class TCPClient implements IClient {

    public static final int POLLING_DELAY = 50;

    private final PrefixLogger logger;
    private final IMessageAcceptor messageAcceptor;
    private final Thread clientThread = new Thread(new Client());

    private MessageStreamer msgStreamer;
    private boolean shutdown = false;
    private boolean isStarted = false;

    public TCPClient(IMessageAcceptor messageAcceptor, Logger logger) {
        this.logger = new PrefixLogger("CL", logger);
        this.messageAcceptor = messageAcceptor;
    }

    public void sendMessage(String message) throws ClientException {
        if (isStarted) {
            try {
                logger.log("Sending message: " + message);
                msgStreamer.sendMsg(message);
            } catch (EncodingException e) {
                logger.log("Unable to send message, closing connection: " + ExceptionExpander.expandException(e));
                shutdown = true;
            }
        } else {
            throw new ClientException("Unable to send message: client is not started");
        }

    }

    public boolean start(String host, int port) {
        if (messageAcceptor == null) {
            logger.log("Unable to start client: no message acceptor specified");
            return false;
        }
        Socket socket;
        try {
            socket = new Socket(host, port);
        } catch (UnknownHostException e) {
            logger.log("Unknown host: " + host);
            return false;
        } catch (IOException e) {
            logger.log("I/O exception while connecting to host: " + e.getMessage());
            return false;
        }

        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            msgStreamer = new MessageStreamer(is, os, logger.getBaseLogger());
        } catch (IOException e) {
            logger.log("Stream mapping failed: " + e.getMessage());
            return false;
        }

        logger.log("Starting client thread...");
        clientThread.start();
        isStarted = true;
        logger.log("Client started up successfuly");
        return true;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void stop() {
        logger.log("Stopping client...");
        long time = System.currentTimeMillis();
        shutdown = true;
        logger.log("Waiting for client thread to terminate");
        try {
            clientThread.join(3000);
        } catch (InterruptedException e) {
            logger.log("Interrupted while waiting client thread to finish");
        }
        isStarted = false;
        double stopTime = System.currentTimeMillis() - time;
        logger.log("Client stopped in " + stopTime / 1000 + " seconds");
    }

    private class Client implements Runnable {
        public void run() {
            while (!shutdown) {
                try {
                    String message = msgStreamer.receiveMsg();
                    if (message != null) {
                        messageAcceptor.acceptMessage("server", message);
                        logger.log("Message received: " + message);
                    } else {
                        Thread.sleep(POLLING_DELAY);
                    }
                } catch (InterruptedException e) {
                    logger.log("Message Streamer interrupted while waiting for a message");
                } catch (EncodingException e) {
                    logger.log("Unable to receive message, closing connection: " + ExceptionExpander.expandException(e));
                    shutdown = true;
//                TODO close or not to close
                }
            }
            logger.log("Client thread stopped");
        }
    }
}
