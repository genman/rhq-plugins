package com.apple.iad.rhq.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

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
public class HttpComponent<T extends ResourceComponent<?>> implements ResourceComponent<T>, MeasurementFacet, HttpBodySource, UrlSource, OperationFacet {

    private static final String UTF_8 = "UTF-8";

    /**
     * Default logger. Logger name changes based on the class name of this component.
     */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * We cache the body from the last 5 seconds.
     * The purpose of this is so multiple sub resources that want to return metrics from the
     * same HTTP page do so without having to fetch the page again.
     */
    private static final int CACHE = 1000 * 5;

    /**
     * URL to monitor by default.
     */
    public static final String PLUGINCONFIG_URL = "url";

    /**
     * RegEx to look for in body.
     */
    public static final String PLUGINCONFIG_REGEX = "regex";

    /**
     * Request body config option.
     */
    public static final String PLUGINCONFIG_BODY = "body";

    /**
     * RegEx to strip from numbers.
     */
    private static final Pattern numberStrip = Pattern.compile("[,\\s]");

    /**
     * Plugin configuration for URL read and connect timeout.
     */
    protected static final String PLUGINCONFIG_TIMEOUT = "timeout";

    /**
     * Plugin configuration for required HTTP status code.
     */
    protected static final String PLUGINCONFIG_STATUS = "status";

    /**
     * Plugin configuration for decoding the response, using this encoding.
     */
    protected static final String PLUGINCONFIG_ENCODING = "encoding";

    /**
     * Measurement; response time (numeric).
     */
    protected static final String RESPONSE_TIME = "responseTime";

    /**
     * Measurement; response size in bytes.
     */
    protected static final String RESPONSE_SIZE = "responseSize";

    /**
     * Measurement; last modified header (numeric).
     */
    protected static final String LAST_MODIFIED = "lastModified";

    /**
     * Configured plugin format.
     */
    public static final String PLUGINCONFIG_FORMAT = "format";

    /**
     * Limit for the response body size.
     * By default, we only read up to 1MB.
     */
    public static final int MAX_RESPONSE_SIZE = 1024 * 1024 * 1024;

    /**
     * HTTP request method.
     * @see Method
     */
    public static final String PLUGINCONFIG_METHOD = "method";

    /**
     * HTTP request character set setting.
     * Uses {@link #PLUGINCONFIG_ENCODING} if not set.
     */
    public static final String PLUGINCONFIG_CHARSET = "charset";

    /**
     * HTTP content type header setting.
     */
    protected static final String PLUGINCONFIG_CONTENT_TYPE = "content-type";

    /**
     * HTTP user agent header setting.
     */
    protected static final String PLUGINCONFIG_USER_AGENT = "user-agent";

    /**
     * If the URI uses this special hostname, substitutes in the actual hostname.
     */
    public static final String CONFIG_HOSTNAME = "hostname";

    private ResourceContext<T> resourceContext;
    private String body = ""; // cached body
    private MeasurementProvider measurementProvider = null; // cached measurementProvider
    private long time = 0; // time to complete request in milliseconds
    private long bodyLength = 0;
    private long last = 0; // time page last fetched
    private Map<String, List<String>> headerFields = Collections.emptyMap();
    private long lastModified = 0;
    private String version;

    /**
     * Different simple document formats supported.
     */
    public enum Format {
        regex,
        json,
        xml,
        jsonTree,
        none
    }

    /**
     * HTTP request methods; supported by this plugin.
     */
    public enum Method {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE
    }

    /**
     * Configured encoding.
     */
    private String encoding = UTF_8;
    private Format format = Format.none;

    private int timeout;

    private Method method = Method.GET;

    private Configuration config;

    private UrlSource parent;

    private int responseCode;

    private String responseMessage;

    /**
     * Default constructor.
     */
    public HttpComponent() {
    }

    /**
     * Construct with a plugin configuration and optional context. This
     * constructor is used for discovery by {@link HttpDiscovery}. The discovery
     * process depends on {@link #getUrl()} to return the URL discovered, which
     * is then tested using {@link #testUrl(URL)}.
     */
    protected HttpComponent(Configuration configuration, ResourceDiscoveryContext context) throws IOException {
        this.config = configuration;
        if (context != null) {
            ResourceComponent rc = context.getParentResourceComponent();
            if (rc instanceof UrlSource) {
                parent = (UrlSource) rc;
            }
        }
        init();
    }

    /**
     * Constructor with configuration and (optional) parent.
     * This is useful for using this class as a simple HTTP client.
     */
    public HttpComponent(Configuration configuration, UrlSource parent) throws IOException {
        this.config = configuration;
        this.parent = parent;
        init();
    }

    /**
     * Construct with a plugin configuration and no context.
     */
    public HttpComponent(Configuration configuration) throws IOException {
        this(configuration, (UrlSource)null);
    }

    public void start(ResourceContext<T> context) throws IOException {
        this.resourceContext = context;
        this.config = this.resourceContext.getPluginConfiguration();
        if (log.isDebugEnabled()) {
            log.debug("starting: " + this);
        }
        ResourceComponent rc = context.getParentResourceComponent();
        if (rc instanceof UrlSource) {
            parent = (UrlSource) rc;
        }
        init();
    }

    private void init() throws IOException {
        this.encoding = config.getSimpleValue(PLUGINCONFIG_ENCODING, UTF_8);
        this.format = Format.valueOf(config.getSimpleValue(PLUGINCONFIG_FORMAT, Format.none.name()));
        this.timeout = getSimpleValue(PLUGINCONFIG_TIMEOUT, 60);
        this.method = Method.valueOf(config.getSimpleValue(PLUGINCONFIG_METHOD, Method.GET.name()));
    }

    public void stop() {
        if (log.isDebugEnabled()) {
            log.debug("stopping: " + this);
        }
    }

    public AvailabilityType getAvailability() {
        boolean result = checkAvailability();
        return result ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    /**
     * Executes an HTTP request and based on the measurement property, collects the appropriate measurement value.
     * This supports measurement data and traits.
     * A metric property is an expression used to extract data from the response body.
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {

        if (!checkAvailability()) {
            // this also stores body locally ...
            log.error("since unavailable, cannot gather metrics " + requests);
            return;
        }

        for (MeasurementScheduleRequest request : requests) {
            String metricPropertyName = request.getName();
            boolean dataMustBeNumeric;

            if (request.getDataType() == DataType.MEASUREMENT) {
                dataMustBeNumeric = true;
            } else if (request.getDataType() == DataType.TRAIT) {
                dataMustBeNumeric = false;
            } else {
                log.error("Plugin does not support metric [" + metricPropertyName + "] of type ["
                    + request.getDataType() + "]");
                continue;
            }
            if (metricPropertyName.equals(RESPONSE_TIME)) {
                report.addData(new MeasurementDataNumeric(request, (double) this.time));
                continue;
            }
            if (metricPropertyName.equals(RESPONSE_SIZE)) {
                report.addData(new MeasurementDataNumeric(request, (double) this.bodyLength));
                continue;
            }
            if (metricPropertyName.equals(LAST_MODIFIED)) {
                report.addData(new MeasurementDataNumeric(request, (double) this.lastModified));
                continue;
            }

            try {

                measurementProvider = getMeasurementProvider(body);

                Object dataValue;
                if (metricPropertyName.startsWith("#")) {
                    metricPropertyName = metricPropertyName.substring(1);
                    List<String> list = headerFields.get(metricPropertyName);
                    if (list == null) {
                        log.error("Header not found [" + metricPropertyName
                            + "] - in headers: " + headerFields.keySet());
                        continue;
                    }
                    dataValue = list.get(0);
                } else {
                    dataValue = measurementProvider.extractValue(metricPropertyName);
                    if (dataValue == null) {
                        log.error("Output did not match metric property [" + metricPropertyName
                            + "] - in body:\n" + truncateString(body));
                        continue;
                    }
                }

                // add the metric value to the measurement report
                if (dataMustBeNumeric) {
                    if (dataValue instanceof Number) {
                        report.addData(new MeasurementDataNumeric(request, ((Number)dataValue).doubleValue()));
                    } else {
                        String s = String.valueOf(dataValue);
                        s = numberStrip.matcher(s).replaceAll("");
                        double numeric = Double.parseDouble(s.trim());
                        report.addData(new MeasurementDataNumeric(request, numeric));
                    }
                } else {
                    String s = String.valueOf(dataValue).trim();
                    report.addData(new MeasurementDataTrait(request, s));
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + metricPropertyName + "]: " + e);
            }
        }
    }

    /**
     * Returns a measurement measurementProvider, for parsing the given body.
     * By default returns a parser based on the message format.
     * Subclasses can customize this.
     */
    protected MeasurementProvider getMeasurementProvider(String body)
            throws Exception
    {
        switch (format) {
        case none:
        case regex: return new RegExProvider(body);
        case json:  return new JSONProvider(body);
        case jsonTree:  return new JSONTreeProvider(body);
        case xml:   return new XPathProvider(body);
        default:    throw new IllegalStateException("null");
        }
    }

    /**
     * Converts the response body into a string.
     * Subclasses (in the case of binary files or the like) may want to override this
     * method to parse this data instead, and return an empty string.
     */
    protected String toString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder(128);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
            int n;
            while ((n = reader.read()) != -1) {
                if (sb.length() > MAX_RESPONSE_SIZE) {
                    log.warn("body over " + MAX_RESPONSE_SIZE + " truncated");
                    break;
                }
                sb.append((char)n);
            }
        } finally {
            is.close();
        }
        return sb.toString();
    }

    /**
     * Converts a URL to a string.
     */
    public static String getBody(URL url) throws IOException {
        HttpComponent httpComponent = new HttpComponent();
        InputStream is = url.openStream();
        return httpComponent.toString(is);
    }

    private boolean checkAvailability() throws InvalidPluginConfigurationException {
        log.debug("checkAvailability");
        URL url = null;
        try {
            url = getUrl();
            boolean avail = testUrl(url);
            return avail;
        } catch (IOException e) {
            log.debug("URL unavailable " + url, e);
            return false;
        } catch (Exception e) {
            throw new InvalidPluginConfigurationException("Bad plugin config: " + e, e);
        }
    }

    /**
     * Returns true if the URL data matches expected content and status; false otherwise.
     * As a side-effect, stores the body and statistics about the URL.
     *
     * @throws IOException for failing to connect to URL or other reasons
     */
    protected boolean testUrl(URL url) throws IOException {
        log.debug("testUrl " + url);
        long start = System.currentTimeMillis();
        URLConnection con = url.openConnection();
        con.setReadTimeout(timeout * 1000);
        con.setConnectTimeout(timeout * 1000);
        InputStream is;
        if (con instanceof HttpURLConnection) {
            HttpURLConnection hcon = (HttpURLConnection)con;
            hcon.setRequestMethod(method.name());
            String userAgent = config.getSimpleValue(PLUGINCONFIG_USER_AGENT, getClass().getName());
            hcon.setRequestProperty(PLUGINCONFIG_USER_AGENT, userAgent);
            byte[] body = getRequestBody();
            if (body.length > 0) {
                log.debug("write body");
                String charset = config.getSimpleValue(PLUGINCONFIG_CHARSET, null);
                if (charset != null)
                    hcon.setRequestProperty(PLUGINCONFIG_CHARSET, charset);
                else
                    charset = encoding;
                String contentType = config.getSimpleValue(PLUGINCONFIG_CONTENT_TYPE, "text/plain");
                hcon.setRequestProperty(PLUGINCONFIG_CONTENT_TYPE, contentType);
                hcon.setDoOutput(true);
                hcon.getOutputStream().write(body);
            }
            int code = getSimpleValue(PLUGINCONFIG_STATUS, 0);
            String msg = "HTTP response code " + hcon.getResponseCode() + " '" + hcon.getResponseMessage() + "'";
            log.debug(msg);
            responseCode = hcon.getResponseCode();
            responseMessage = hcon.getResponseMessage();
            if (code != 0 && responseCode != code) {
                log.debug(msg + " not " + code);
                return false;
            }
            if (responseCode >= 400) {
                is = hcon.getErrorStream();
                if (is == null)
                    is = new ByteArrayInputStream(new byte[0]);
            } else {
                is = hcon.getInputStream();
            }
        } else {
            is = con.getInputStream();
        }

        log.debug("read body");
        this.body = toString(is);
        this.time = System.currentTimeMillis() - start;
        this.lastModified = con.getLastModified();
        this.bodyLength = con.getContentLength();
        this.headerFields = con.getHeaderFields();
        this.version = con.getHeaderField("Server");
        this.last = System.currentTimeMillis();

        return validateBody(body);
    }

    /**
     * Return the last response code.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Return the last response message.
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * Validates the body content, returning true if the body matches a configured regular expression.
     * Subclasses may override this method to change validation.
     *
     * @throws IOException if parsing fails
     *
     * @see #PLUGINCONFIG_REGEX
     */
    protected boolean validateBody(String body) throws IOException {
        String regex = config.getSimpleValue(PLUGINCONFIG_REGEX, null);
        if (regex != null && !regex.isEmpty()) {
            Pattern pattern = Pattern.compile(regex);
            if (log.isDebugEnabled())
                log.debug("content '" + truncateString(body) + "' regex '" + pattern.pattern() + "'");
            Matcher match = pattern.matcher(body);
            if (match.find()) {
                log.debug("Found");
            } else {
                log.debug("NOT found");
                return false;
            }
        }
        return true;
    }

    private int getSimpleValue(String s, int i) {
        String value = config.getSimpleValue(s, null);
        if (value == null)
            return i;
        return Integer.parseInt(value);
    }

    /**
     * Truncate a string so it is short, usually for display or logging purposes.
     *
     * @param output the output to trim
     * @return the trimmed output
     */
    private String truncateString(String output) {
        String outputToLog = output;
        if (outputToLog != null && outputToLog.length() > 100) {
            outputToLog = outputToLog.substring(0, 100) + "...";
        }
        return outputToLog;
    }

    @Override
    public BufferedReader getBodyReader() {
        return new BufferedReader(new StringReader(getBody()));
    }

    @Override
    public String getBody() {
        if (last < System.currentTimeMillis() - CACHE) {
            checkAvailability();
        }
        return body;
    }

    @Override
    public MeasurementProvider getMeasurementProvider() throws Exception {
        getBody();
        if (body == null)
            throw new IllegalStateException("null body");
        if (measurementProvider == null || measurementProvider.getBody() != body) {
            measurementProvider = getMeasurementProvider(body);
        }
        return measurementProvider;
    }

    /**
     * Clears any cached data state; to save memory or to immediately refetch data again.
     */
    public void clearState() {
        body = "";
        measurementProvider = null;
        time = 0;
        bodyLength = 0;
        last = 0;
        headerFields = Collections.emptyMap();
        lastModified = 0;
        version = null;
    }

    /**
     * Returns an optional request body, obtained by calling {@link #getRequestBodyString()}.
     * This is only used for PUT and POST.
     * The string is converted into a byte array using the configured character encoding.
     *
     * @see #PLUGINCONFIG_BODY
     * @see #PLUGINCONFIG_CHARSET
     *
     * @throws IOException if body could not be encoded
     */
    protected byte[] getRequestBody() throws IOException {
        String charset = config.getSimpleValue(PLUGINCONFIG_CHARSET, encoding);
        log.debug("getRequestBody");
        return getRequestBodyString().getBytes(charset);
    }

    /**
     * Returns an optional request body, obtained by default from configuration.
     * This is only used for PUT and POST.
     */
    protected String getRequestBodyString() {
        log.debug("getRequestBodyString");
        return config.getSimpleValue(PLUGINCONFIG_BODY, "");
    }

    void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Returns the resource context for this component.
     */
    protected final ResourceContext<T> getResourceContext() {
        return resourceContext;
    }

    /**
     * Returns the monitored URL, obtained by the plugin config by default.
     * Subclasses may override this method to produce whatever monitored URL to use.
     * If the configured URL is in fact a URI, uses the {@link #getParentUrl()} method
     * to obtain it first.
     *
     * @throws IOException if URL creation failed
     */
    public URL getUrl() throws IOException {
        String s = config.getSimpleValue(PLUGINCONFIG_URL, null);
        if (s == null) {
            throw new InvalidPluginConfigurationException("Missing url in plugin config for " + config);
        }
        return resolveUrl(s);
    }

    /**
     * Resolves the give URL from a string.
     */
    protected URL resolveUrl(String urlStr) throws IOException {
        try {
            URI uri = new URI(urlStr);
            log.debug("uri " + uri.getHost());
            if (CONFIG_HOSTNAME.equals(uri.getHost())) {
                String hostname = InetAddress.getLocalHost().getHostName();
                uri = new URI(uri.getScheme(), uri.getUserInfo(), hostname,
                        uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            }
            if (uri.isAbsolute()) {
                log.debug("absolute URL");
                return uri.toURL();
            } else {
                UrlSource us = getParentUrlSource();
                if (us == null)
                    throw new IOException("parent URL source null " + parent);
                URL purl = us.getUrl();
                URL url = purl.toURI().resolve(uri).toURL();
                if (log.isDebugEnabled()) {
                    log.debug("relative URL parent " + purl + " this " + uri);
                    log.debug("result " + url);
                }
                return url;
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the version of this component; by default the version from HTTP server.
     */
    protected String getVersion() {
        return version;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Return the plugin configuration.
     */
    protected final Configuration getConfiguration() {
        return config;
    }

    /**
     * For nested HTTP components, returns the parent component's configured URL.
     */
    protected UrlSource getParentUrlSource() {
        return parent;
    }

    /**
     * Debug string.
     */
    @Override
    public String toString() {
        URL url = null;
        try {
            url = getUrl();
        } catch (Exception e) {}
        return getClass().getSimpleName() + " [url=" + url + "]";
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
        String extract = parameters.getSimpleValue("extract", "");
        if (name.equals("test")) {
            URL url = getUrl();
            boolean success = testUrl(url);
            OperationResult operationResult = new OperationResult();
            Configuration c = operationResult.getComplexResults();
            c.setSimpleValue("success", Boolean.toString(success));
            c.setSimpleValue("responseCode", String.valueOf(responseCode));
            c.setSimpleValue("url", url.toString());
            c.setSimpleValue("request", getRequestBodyString());
            if (!extract.isEmpty()) {
                Object value = getMeasurementProvider().extractValue(extract);
                c.setSimpleValue("response", value != null ? value.toString() : "");
            } else {
                c.setSimpleValue("response", body);
            }
            return operationResult;
        }
        throw new UnsupportedOperationException();
    }

}
