package ru.ifmo.team.filenetwork;

/**
 * User: Daniel Penkin
 * Date: May 5, 2009
 * Version: 1.0
 */
public class MessagingException extends Exception {

    public MessagingException(String s) {
        super(s);
    }

    public MessagingException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
