package com.apple.iad.rhq.http;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Treats the data as XML, and extracts values that are an XPath expressions.
 */
public class XPathProvider extends MeasurementProvider {

    private final ThreadLocal<DocumentBuilder> dbf = new ThreadLocal<DocumentBuilder>() {

        @Override
        protected DocumentBuilder initialValue() {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // never forget this!
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new Error(e);
            }
        }

    };

    private final ThreadLocal<XPathFactory> xf = new ThreadLocal<XPathFactory>() {

        @Override
        protected XPathFactory initialValue() { return XPathFactory.newInstance(); }

    };

    private final Document document;

    /**
     * Constructs a new instance; parsing the body immediately.
     */
    public XPathProvider(String body) throws SAXException, IOException {
        super(body);
        DocumentBuilder builder = dbf.get();
        document = builder.parse(new InputSource(new StringReader(body)));
    }

    /**
     * Extracts a value that's an XPath expression.
     */
    @Override
    public String extractValue(String metricPropertyName) {
        XPath xpath = xf.get().newXPath();
        try {
            XPathExpression expr = xpath.compile(metricPropertyName);
            String str = (String) expr.evaluate(document, XPathConstants.STRING);
            return str;
        } catch (XPathExpressionException e) {
            log.warn("cannot compile " + metricPropertyName, e);
            return null;
        }
    }

    /**
     * Returns the parsed XML document.
     */
    public Document getDocument() {
        return document;
    }

}
