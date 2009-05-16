package ru.ifmo.team.util.tcp.client;

import ru.ifmo.team.util.IMessageAcceptor;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
public interface IClient {

    void sendMessage(String message) throws ClientException;

    boolean start(IMessageAcceptor messageAcceptor, String host, int port);

    boolean isStarted();

    void stop();
}
