package ru.ifmo.team.util.tcp.server;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
public class ServerException extends Exception {
    public ServerException(String s) {
        super(s);
    }

    public ServerException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
