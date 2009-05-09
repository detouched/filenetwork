package ru.ifmo.team.util.tcp.server;

/**
 * User: Daniel Penkin
 * Date: Apr 27, 2009
 * Version: 1.0
 */
public interface IServer {

    boolean start();

    boolean isStarted();

    void stop();

}
