package ru.ifmo.team.util.tcp.server;

import ru.ifmo.team.util.IClientManager;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Daniel Penkin
 * Date: Apr 27, 2009
 * Version: 1.0
 */
public class TCPServer implements IServer {

    private final int port;
    private final int timeout;
    private final int acceptTimeout;
    private final PrefixLogger logger;
    private final Thread serverThread = new Thread(new Server());
    private final Map<String, ConnectionHandler> handlers = new HashMap<String, ConnectionHandler>();
    private final IClientManager clientManager;

    private long startTime;
    private ServerSocket serverSocket;
    private boolean shutdown = true;
    private boolean isStarted = false;


    public TCPServer(int port, int timeout, int acceptTimeout, IClientManager clientManager, Logger logger) {
        this.port = port;
        this.timeout = timeout;
        this.acceptTimeout = acceptTimeout;
        this.clientManager = clientManager;
        this.logger = new PrefixLogger("SRV", logger);
    }


    public boolean start() {
        logger.log("Starting server @ port " + port);
        startTime = System.currentTimeMillis();
        shutdown = false;
        if (clientManager == null) {
            logger.log("Unable to start server: no client manager specified");
            return false;
        }
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(acceptTimeout);
            logger.log("Port " + port + " binded");
        } catch (IOException e) {
            logger.log("Unable to bind port");
            return false;
        }

        logger.log("Starting listening port " + port);
        serverThread.start();
        isStarted = true;
        logger.log("Server started up successfully");
        return true;
    }


    public void stop() {
        logger.log("Stopping server...");
        long time = System.currentTimeMillis();
        shutdown = true;
        logger.log("Waiting for server thread to terminate");

        try {
            serverThread.join(acceptTimeout + 1000);
        } catch (InterruptedException e) {
            logger.log("Interrupted while waiting server thread to finish");
        }

        for (Map.Entry<String, ConnectionHandler> connection : handlers.entrySet()) {
            try {
                ConnectionHandler handler = connection.getValue();
                handler.shutDown();
                handler.join(timeout + 1000);
            } catch (InterruptedException e) {
                logger.log("Server interrupted while waiting for handlers' finish: " + e.getMessage());
            }
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.log("Unable to close server socket properly: " + e.getMessage());
            }
        }
        isStarted = false;
        double stopTime = System.currentTimeMillis() - time;
        logger.log("Server stopped in " + stopTime / 1000 + " seconds");
        stopTime = System.currentTimeMillis() - startTime;
        logger.log("Server work time: " + stopTime / 1000 + " seconds");

    }

    public boolean isStarted() {
        return isStarted;
    }

    /*package*/ void unregisterConnection(String ip) {
        if (!shutdown) {
            handlers.remove(ip);
        }
    }

    private class Server implements Runnable {

        public void run() {
            while (!shutdown) {
                Socket socket = null;
                try {
                    logger.log("Waiting for connection for " + acceptTimeout + " milliseconds. "
                            + "Count of active connections: " + handlers.size());
                    socket = serverSocket.accept();
                    socket.setSoTimeout(timeout);
                    logger.log("Remote address connected: " + socket.getRemoteSocketAddress());                    
                } catch (IOException e) {
                    logger.log("No connection established");
                }

                if (socket != null) {
                    ConnectionHandler handler = new ConnectionHandler(socket, TCPServer.this, logger.getBaseLogger());
                    handler.start();
                    clientManager.clientJoined(socket.getRemoteSocketAddress().toString(), handler);
                    String ip = socket.getRemoteSocketAddress().toString();
                    handlers.put(ip, handler);
                }
            }
        }
    }

}
