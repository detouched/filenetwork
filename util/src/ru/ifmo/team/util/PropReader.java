package ru.ifmo.team.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Daniel Penkin
 * Date: May 12, 2009
 * Version: 1.0
 */
public class PropReader {

    public static Map<String, String> readProperties(File file) throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() < 1) continue;
            char ch = line.charAt(0);
            if (ch == '#') {
                continue; // Comment
            }
            if (ch == '\uFEFF') line = line.substring(1); // Remove BOM
            int sp = line.indexOf('=');
            if (sp >= 0) {
                String key = line.substring(0, sp);
                props.put(key.trim().toLowerCase(), line.substring(sp + 1));
            }
        }
        return props;
    }
}
