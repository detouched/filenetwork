package ru.ifmo.team.util;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
public class ExceptionExpander {

    public static String expandException(Exception e) {
        String result = e.getMessage();
        Throwable throwable = e.getCause();
        while (throwable != null) {
            result += " CAUSED BY " + throwable.getMessage();
            throwable = throwable.getCause();
        }
        return result;
    }
}
