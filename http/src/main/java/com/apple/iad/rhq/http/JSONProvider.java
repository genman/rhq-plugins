package com.apple.iad.rhq.http;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Parses a JSon document at a URL.
 */
public class JSONProvider extends MeasurementProvider {

    private final JSONParser parser = new JSONParser();
    private final Map<String, Object> document;

    /**
     * Parses a JSon document.
     * @throws ParseException
     * @throws IOException
     */
    public JSONProvider(String body) throws IOException, ParseException {
        super(body);
        this.document = parseDocument();
    }

    /**
     * Creates a association between every JSon map key and value, even nested.
     *
     * @return mapping of associations
     */
    private Map<String, Object> parseDocument() throws IOException, ParseException {
        Reader r = new StringReader(body);
        try {
            Map<String, Object> digest = new HashMap<String, Object>();
            Object o = parser.parse(r);
            digestTree(digest, o);
            return digest;
        } finally {
            r.close();
        }
    }

    /**
     * Returns the backing document.
     */
    public Map<String, Object> getDocument() {
        return document;
    }

    static void digest(Map<String, Object> values, String s) throws ParseException {
        JSONParser parser = new JSONParser();
        Object o = parser.parse(s);
        if (o instanceof Map) {
            Map<Object, Object> parse = (Map) o;
            digestTree(values, parse);
        }
    }

    /**
     * Iterate over the data structure. If a mapped value is encountered
     * that is a string, assign it to the first 'values' parameter
     * as a Double.
     * @param values
     * @param value
     */
    static void digestTree(Map<String, Object> values, Object value) {
        if (value instanceof Map) {
            Map<Object, Object> m = (Map)value;
            for (Entry<Object, Object> e : m.entrySet()) {
                Object o = e.getValue();
                if (o instanceof Number) {
                    values.put(e.getKey().toString(), o);
                    // values.put(e.getKey().toString(), ((Number)o).doubleValue());
                } else if (o instanceof String) {
                    String s = (String)o;
                    if (s.isEmpty())
                        continue;
                    values.put(e.getKey().toString(), s);
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

    @Override
    public Object extractValue(String metricPropertyName) {
        return document.get(metricPropertyName);
    }

}
