package ru.ifmo.team.util;

/**
 * User: Daniel Penkin
 * Date: Apr 29, 2009
 * Version: 1.0
 */
public class ExceptionExpander {

    public static String expandException(Exception e) {
        Throwable throwable = e;
        String result = "";
        while (throwable.getCause() != null) {
            result += throwable.getMessage();
            if (throwable.getCause() != null) {
                result += " CAUSED BY ";
                throwable = throwable.getCause();
            }
        }
        result += throwable.getMessage();
        return result;
    }
}
