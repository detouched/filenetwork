package ru.ifmo.team.util.tcp.server;

import ru.ifmo.team.util.IMessageAcceptor;

/**
 * User: Daniel Penkin
 * Date: May 7, 2009
 * Version: 1.0
 */
public interface IConnectionHandler {

    void sendMessage(String message) throws ServerException;

    void registerMessageAcceptor(IMessageAcceptor messageAcceptor);

    void shutDown();
}
