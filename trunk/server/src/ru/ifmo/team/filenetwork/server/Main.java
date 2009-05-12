package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.util.PropReader;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.server.TCPServer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * User: Daniel Penkin
 * Date: May 7, 2009
 * Version: 1.0
 */
public class Main {

    private static final String LOG_FILE_NAME = "server";
    private static final String PROPERTIES_FILE_NAME = "server.properties";
    private static final int DEF_SOCKET_TIMEOUT = 10000;
    private static final int DEF_ACCEPT_TIMEOUT = 5000;

    private int port;
    private int acceptTimeout;
    private int socketTimeout;

    private void run() throws InterruptedException {
        Map<String, String> props = null;
        try {
            props = PropReader.readProperties(new File(PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            System.out.println("Cannot read properties file (" + PROPERTIES_FILE_NAME + "): " + e.getMessage());
            System.exit(-1);
        }

        String logFile = LOG_FILE_NAME;

        if (props != null) {
            String logFolder = props.get("log_folder");
            if (logFolder != null) {
                logFile = logFolder + logFile;
            } else {
                System.out.println("log_folder property not found, using current folder for logging");
            }
            try {
                port = Integer.parseInt(props.get("port"));
            } catch (NumberFormatException e) {
                port = 0;
            }
            if (port == 0) {
                System.out.println("port property not found or set incorrect");
                System.exit(-1);
            }
            try {
                socketTimeout = Integer.parseInt(props.get("socket_timeout"));
            } catch (NumberFormatException e) {
                System.out.println("socket_timeout property not found, using default value (" +
                        DEF_SOCKET_TIMEOUT + ")");
                socketTimeout = DEF_SOCKET_TIMEOUT;
            }
            try {
                acceptTimeout = Integer.parseInt(props.get("accept_timeout"));
            } catch (NumberFormatException e) {
                System.out.println("accept_timeout property not found, using default value (" +
                        DEF_ACCEPT_TIMEOUT + ")");
                acceptTimeout = DEF_ACCEPT_TIMEOUT;
            }
        } else {
            System.out.println("Properties were not read properly");
            System.exit(-1);
        }

        logFile += "_" + System.currentTimeMillis() % 1000;

        String log = logFile + ".log";
        String tcpLog = logFile + "_tcp.log";
        System.out.println("Trying to log into Server log: " + log);
        System.out.println("Trying to log into Server_tcp log: " + tcpLog);
        Logger serverLogger = new Logger(log);
        Logger serverTCPLogger = new Logger(tcpLog);

        serverLogger.clearLog();
        serverTCPLogger.clearLog();
        IFileServer fileServer = new FileServer(serverLogger);
        TCPServer server = new TCPServer(port, socketTimeout, acceptTimeout, fileServer, serverTCPLogger);
        System.out.print("Starting server...");
        if (server.start()) {
            System.out.println("OK");
        } else {
            System.out.println("FAILED");
            System.out.println("Unable to start TCP server, see log for detailed information");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Main().run();
    }
}
