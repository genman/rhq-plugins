package com.apple.iad.rhq.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts data using regular expressions.
 */
public class RegExProvider extends MeasurementProvider {

    public RegExProvider(String body) {
        super(body);
    }

    /**
     * Extracts the value of the metric; using the body.
     */
    public String extractValue(String metricPropertyName) {
        Pattern pattern = Pattern.compile(metricPropertyName);
        Matcher match = pattern.matcher(body);
        if (match.find()) {
            if (match.groupCount() > 0) {
                return match.group(1);
            } else {
                return match.group();
            }
        }
        return null; // not found
    }


}
