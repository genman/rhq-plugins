package com.apple.iad.rhq.hadoop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Parses a JSon document at a URL.
 */
public class JSONProvider {
    
    private final URL url;
    private final JSONParser parser = new JSONParser();

    /**
     * Parses a JSon document at a URL.
     */
    public JSONProvider(URL url) {
        this.url = url;
    }

    /**
     * Creates a association between every JSon map key and value, even nested.
     * 
     * @return mapping of associations
     * @throws IOException
     * @throws ParseException
     */
    Map<String, Double> getDocument() throws IOException, ParseException {
        InputStreamReader isr = new InputStreamReader(url.openStream());
        try {
            Map<String, Double> digest = new HashMap<String, Double>();
            Object o = parser.parse(isr);
            digestTree(digest, o);
            return digest;
        } finally {
            isr.close();
        }
    }
    
    static void digest(Map<String, Double> values, String s) throws ParseException {
        JSONParser parser = new JSONParser();
        Object o = parser.parse(s);
        if (o instanceof Map) {
            Map<Object, Object> parse = (Map) o;
            digestTree(values, parse);
        }
    }

    /**
     * Iterate over the datastructure. If a mapped value is encountered
     * that is a string, assign it to the first 'values' parameter
     * as a Double.
     * @param values
     * @param value
     */
    static void digestTree(Map<String, Double> values, Object value) {
        if (value instanceof Map) {
            Map<Object, Object> m = (Map)value;
            for (Entry<Object, Object> e : m.entrySet()) {
                Object o = e.getValue();
                if (o instanceof Number) {
                    values.put(e.getKey().toString(), ((Number)o).doubleValue());
                } else if (o instanceof String) {
                    String s = (String)o;
                    if (s.isEmpty())
                        continue;
                    try {
                        values.put(e.getKey().toString(), new Double(s));
                    } catch (NumberFormatException ex) {
                        // ignore
                    }
                } else {
                    digestTree(values, o);
                }
            }
        }
        if (value instanceof List) {
            List l = (List)value;
            for (Object o : l)
                digestTree(values, o);
        }
    }

}
