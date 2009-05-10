package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.server.TCPServer;

/**
 * User: Daniel Penkin
 * Date: May 7, 2009
 * Version: 1.0
 */
public class Main {

    private static final Logger serverLogger = new Logger("/Users/danielpenkin/Desktop/logs/server.log");
    private static final Logger tcpServerLogger = new Logger("/Users/danielpenkin/Desktop/logs/server_tcp.log");

    private void run() throws InterruptedException {
        serverLogger.clearLog();
        tcpServerLogger.clearLog();
        IFileServer fileServer = new FileServer(serverLogger);
        TCPServer server = new TCPServer(5555, 1000, 3000, fileServer, tcpServerLogger);
        System.out.print("Starting server...");
        if (server.start()) {
            System.out.println("OK");
//            Thread.sleep(30000);
//            System.out.print("Stopping server...");
//            fileServer.shutDown();
//            server.stop();
        } else {
            System.out.println("FAILED");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Main().run();
    }
}
