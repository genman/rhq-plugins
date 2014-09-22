package com.apple.iad.rhq.http;

import java.io.IOException;
import java.io.InputStream;

import org.json.simple.parser.ParseException;


/**
 * Represents a managed resource identified by a URL.
 * This component can actually be more than an HTTP URL; it supports any URL at all.
 * <p>
 * Subclasses can parse measurements in a different format by overriding {@link #getMeasurementProvider(String)}.
 * Subclasses can adjust the URL used by overriding {@link #getUrl()}.
 * Subclasses can configure how the body is validated by overriding {@link #validateBody(String)()}.
 * </p>
 *
 * @author Elias Ross
 */
public class SubclassComponent extends HttpComponent {

    public static final String BODY  = "{\"foo\":3}";
    public static final String BODY2 = "{\"bar\":3}";

    @Override
    protected MeasurementProvider getMeasurementProvider(String body) throws IOException, ParseException {
        assert body == BODY;
        MeasurementProvider mp = new JSONProvider(BODY);
        return mp;
    }

    @Override
    protected String toString(InputStream is) throws IOException {
        return BODY;
    }

    @Override
    protected boolean validateBody(String body) throws IOException {
        assert body == BODY;
        return true;
    }

    @Override
    protected byte[] getRequestBody() throws IOException {
        byte b[] = super.getRequestBody();
        assert new String(b).equals(BODY2);
        return b;
    }

    @Override
    protected String getRequestBodyString() {
        return BODY2;
    }

    @Override
    protected UrlSource getParentUrlSource() {
        return super.getParentUrlSource();
    }

}
