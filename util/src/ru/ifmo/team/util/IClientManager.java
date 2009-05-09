package ru.ifmo.team.util;

import ru.ifmo.team.util.tcp.server.IConnectionHandler;

/**
 * User: Daniel Penkin
 * Date: May 7, 2009
 * Version: 1.0
 */
public interface IClientManager {

    IMessageAcceptor clientJoined(String ip, IConnectionHandler connectionHandler);
    
}
