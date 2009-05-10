package ru.ifmo.team.util;

/**
 * User: Daniel Penkin
 * Date: Apr 27, 2009
 * Version: 1.0
 */
public interface IMessageAcceptor {

    void acceptMessage(String ip, String message);

    void connectionClosed(String ip);

}
