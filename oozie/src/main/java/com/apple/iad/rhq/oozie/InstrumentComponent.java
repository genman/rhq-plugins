package com.apple.iad.rhq.oozie;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import com.apple.iad.rhq.http.HttpComponent;
import com.apple.iad.rhq.http.MeasurementProvider;

/**
 * Holds instrumentation information.
 */
public class InstrumentComponent extends HttpComponent {

    private final static Log log = LogFactory.getLog(InstrumentComponent.class);

    private Map<String, List<Map>> json = Collections.emptyMap();

    enum Scope { variables, counters, samplers, timers }

    protected MeasurementProvider getMeasurementProvider(String body)
            throws IOException, ParseException, SAXException
    {
        this.json = (Map<String, List<Map>>) new JSONParser().parse(body);
        return new MeasurementProvider(body) {

            @Override
            public String extractValue(String metricPropertyName) {
                String[] split = metricPropertyName.split("\\.");
                return value(Scope.valueOf(split[0]), split[1], split[2]);
            }
        };
    }

    /**
     * This code is a disaster but so is the JSon.
     */
    private String value(Scope scope, String group, String name) {
        boolean debug = log.isDebugEnabled();
        if (debug)
            log.debug("groups " + json.keySet());
        List<Map> groups = json.get(scope.name());
        if (groups == null)
            return null;
        for (Map g : groups) {
            if (group.equals(g.get("group"))) {
                List<Map> lm = (List<Map>)g.get("data");
                if (debug)
                    log.debug("group " + lm);
                for (Map g2 : lm) {
                    if (name.equals(g2.get("name"))) {
                        log.debug("found");
                        return g2.get("value").toString();
                    }
                }
            }
        }
        log.debug("not found");
        return null;
    }

    /**
     * Return the JSON from the parent.
     */
    public Object getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "InstrumentComponent [json=" + json.keySet() + "]";
    }

}
