package com.apple.iad.rhq.http;

import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Parses a JSon document at a URL.
 */
public class JSONTreeProvider extends MeasurementProvider {

    private final Object tree;

    /**
     * Parses a JSon document.
     * @throws ParseException
     * @throws IOException
     */
    public JSONTreeProvider(String body) throws IOException, ParseException {
        super(body);
        JSONParser parser = new JSONParser();
        Object parse = parser.parse(new StringReader(body));
        if (!(parse instanceof Map))
            throw new ParseException(0, "expected Map " + parse);
        tree = parser.parse(new StringReader(body));
    }

    @Override
    public Object extractValue(String property) {
        if (property.isEmpty())
            return null;
        String split = property.substring(0, 1);
        String[] es = property.split(split);
        Object ctree = tree;
        for (int i = 1; i < es.length; i++) {
            if (ctree instanceof List) {
                List l = (List) ctree;
                ctree = l.get(parseInt(es[i]));
            } else if (ctree instanceof Map) {
                ctree = ((Map)ctree).get(es[i]);
            } else {
                return null;
            }
        }
        return ctree;
    }

    /**
     * Returns the parse tree.
     */
    public Object getTree() {
        return tree;
    }

    @Override
    public String toString() {
        return "JSONTreeProvider [tree=" + tree + "]";
    }

}
