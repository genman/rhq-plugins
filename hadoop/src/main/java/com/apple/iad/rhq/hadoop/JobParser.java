package com.apple.iad.rhq.hadoop;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.HTMLConfiguration;

/**
 * Parses the HTML for the job tracker queue.
 */
class JobParser extends AbstractSAXParser {

    private static Pattern num = Pattern.compile("[0-9]+");

    private static final String TD = "TD";
    private static final String TR = "TR";

    boolean td = false;
    int tr = 0;
    List<Integer> list = new ArrayList<Integer>();

    int count = 0;
    int col[] = { 0, 0, 0, 0 };

    /**
     * Constructs a new instance.
     */
    public JobParser() {
        super(new HTMLConfiguration());
    }

    @Override
    public void startElement(QName name, XMLAttributes attr, Augmentations aug) throws XNIException {
        if (name.rawname.equals(TR)) {
            tr++;
        } else if (name.rawname.equals(TD)) {
            td = true;
            String id = attr.getValue("id");
            if (id != null && id.startsWith("job")) {
            }
        } else {
            td = false;
        }
    }

    @Override
    public void endElement(QName name, Augmentations aug) throws XNIException {
        if (name.rawname.equals(TR)) {
            tr--;
        }
        if (tr == 0) {
            if (list.size() == 4) {
                for (int i = 0; i < list.size(); i++)
                    col[i] += list.get(i);
                count++;
            }
            list.clear();
        }
    }

    @Override
    public void characters(XMLString str, Augmentations aug) throws XNIException {
        String s = str.toString().trim();
        if (td && num.matcher(s).matches()) {
            list.add(Integer.parseInt(s));
        }
    }

}