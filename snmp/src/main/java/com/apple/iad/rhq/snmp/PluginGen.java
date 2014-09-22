package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.percederberg.mibble.MibType;
import net.percederberg.mibble.MibTypeTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Generates an rhq-plugin.xml file using data compiled from a vendor provided MIB.
 */
public class PluginGen {

    private Log log = LogFactory.getLog(getClass());

    public static final int MAX_DESC_LEN = 1000;

    public static final String URN_C = "urn:xmlns:rhq-configuration";

    // BUG: Namespace should be by default 'rhq', but can't do it
    // Search Google for details... could strip 'rhq' in the resulting XML I guess
    private static final Namespace DN = Namespace.getNamespace("rhq", "urn:xmlns:rhq-plugin");
    private static final Namespace C = Namespace.getNamespace("c", URN_C);

    /**
     * True to guess:
     * 1) Based on description in name treat OCTET string as measurement anyway
     */
    boolean guess = true;

    static String join(String delimiter, String ... args) {
        StringBuilder builder = new StringBuilder();
        Iterator iter = Arrays.asList(args).iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext())
                break;
            builder.append(delimiter);
        }
        return builder.toString();
    }

    private final String mibs;
    private final MibIndex index;

    /**
     * Reads in MIB filenames as args, outputs to the writer generated XML.
     *
     * @throws IOException if indexing the MIB failed
     */
    public PluginGen(String args[]) throws IOException {
        index = new MibIndex();
        index.setExtra(true);
        for (String arg : args) {
            index.load(arg);
        }
        mibs = join(", ", args);
    }

    /**
     * Calls {@link PluginGen} with the list of MIBs as strings.
     */
    public static void main(String args[]) throws IOException {
        PluginGen pg = new PluginGen(args);
        pg.output(new PrintWriter(System.out));
    }

    public void output(Writer writer) throws IOException {
        Document doc = new Document();
        Element plugin = new Element("plugin");
        plugin.addNamespaceDeclaration(DN);
        plugin.addNamespaceDeclaration(C);
        plugin.setAttribute("name", "short name");
        plugin.setAttribute("displayName", "Longer name");
        plugin.setAttribute("description", "What this does");
        plugin.setAttribute("package", "com.apple.iad.rhq.snmp");
        plugin.addContent(new Element("depends").setAttribute("plugin", "snmp").setAttribute("useClasses", "true") );
        // displayName="SNMP Monitoring Plugin" description="Monitor SNMP" package="com.apple.iad.rhq.snmp
        Element service = new Element("service");
        Element ri = new Element("runs-inside");
        Element prt = new Element("parent-resource-type").setAttribute("name", "SNMP Component").setAttribute("plugin", "snmp");
        ri.addContent( prt );
        Element pc = new Element("plugin-configuration");
        Element sp = new Element("simple-property", C);
        plugin.addContent(service);
        service.addContent(ri);
        service.addContent(pc);
        pc.addContent(sp);
        service.setAttribute("name", "XXX TODO XXX").setAttribute("discovery", "MibDiscovery").
            setAttribute("class", "MibComponent").setAttribute("singleton", "true").
            setAttribute("classLoader", "instance");
        sp.setAttribute("name", "mibs").setAttribute("default", mibs);
        doc.setRootElement(plugin);

        // Exclude table columns from top level list
        Set<String> tnames = new HashSet<String>();

        for (Map.Entry<String, TableRecord> me : index.getTables().entrySet()) {
            tnames.add(me.getKey());
            for (String col : me.getValue().getColumns()) {
                tnames.add(col);
            }
        }

        for (Map.Entry<String, NameRecord> me : index.getNames().entrySet()) {
            NameRecord nr = me.getValue();
            MibType syntax = nr.getSyntax();
            String col = me.getKey();
            log.debug(col + " " + syntax);
            if (tnames.contains(col))
                continue;
            addMeasurement(service, col);
        }

        for (Map.Entry<String, TableRecord> me : index.getTables().entrySet()) {
            String tableName = me.getKey();
            TableRecord tableRecord = me.getValue();
            Element tservice = new Element("service");
            tservice.setAttribute("name", tableName).setAttribute("discovery", "MibTableDiscovery").
                setAttribute("class", "MibTableComponent").setAttribute("singleton", "false");
            service.addContent(tservice);
            for (String col : tableRecord.getColumns()) {
                addMeasurement(tservice, col);
            }
        }

        Format f = Format.getPrettyFormat();
        f.setLineSeparator("\n");
        f.setIndent("    ");
        XMLOutputter out = new XMLOutputter(f);
        out.output(doc, writer);
        writer.flush();
    }

    private void addMeasurement(Element service, String name) {
        log.debug("addMeasurement " + name);
        NameRecord nameRecord = index.getNameRecord(name);
        if (nameRecord == null) {
            log.error("unknown name " + name);
            return;
        }
        if (nameRecord.getAccess() == null) {
            log.debug("name getAccess() null " + name);
            return;
        }
        if (!nameRecord.getAccess().canRead()) {
            log.debug("cannot read");
            return;
        }
        MibTypeTag tag = nameRecord.getSyntax().getTag();
        String dataType;
        String desc = nameRecord.getDesc();
        String units = null;
        boolean trait = false; // true if probably a trait
        boolean last = false;
        desc = desc.replaceAll("[\\n\\r\\t]", " ");
        desc = desc.replaceAll(" +", " ");
        if (guess) {
            if (desc.matches("(?i).*\\b(number|count).*"))
                tag = MibTypeTags.Gauge;
            if (desc.matches("(?i).*\\btotal.*"))
                tag = MibTypeTags.Counter;
            units = units(desc);
            if (desc.matches("(?i).*\\b(last|recent|previous|prior)"))
                last = true;
            if (units == null && tag == nameRecord.getSyntax().getTag() && trait(desc)) {
                trait = true;
            }

            log.debug("desc " + desc);
            log.debug("trait " + trait + " units " + units + " tag " + tag);
        }
        if (desc.length() > MAX_DESC_LEN) {
            desc = desc.substring(0, MAX_DESC_LEN);
        }
        Map<Integer, String> mapping = index.getMapping(name);
        boolean hasMapping = mapping != null && !mapping.isEmpty();
        if (hasMapping) {
            Comment c = new Comment(name + " values: " + mapping);
            service.addContent(c);
            trait = true;
        }
        if (MibTypeTags.isMeasurement(tag) && !trait) {
            dataType = "measurement";
        } else {
            dataType = "trait";
        }
        Element metric = new Element("metric");
        metric.setAttribute("property", name).
            setAttribute("description", desc).
            setAttribute("displayType", "detail").
            setAttribute("dataType", dataType);
        if (MibTypeTags.Counter.equals(tag) && !last) {
            metric.setAttribute("measurementType", "trendsup");
        }
        if (MibTypeTags.TimeTicks.equals(tag)) {
            metric.setAttribute("units", "milliseconds");
        } else if (MibTypeTags.TimeTicks.equals(tag)) {
            metric.setAttribute("units", "milliseconds");
        } else if (units != null) {
            metric.setAttribute("units", units);
        }
        service.addContent(metric);
    }

    static boolean trait(String desc) {
        return desc.matches("(?i).*\\b(port|version|id\\b|identi|indicat|status|state).*");
    }

    static String units(String desc) {
        if (desc.matches("(?i).*\\bk(ilo)?bytes.*") || desc.matches(".*\\bKB.*"))
            return "kilobytes";
        if (desc.matches("(?i).*\\bm(ega)?bytes.*") || desc.matches(".*\bMB.*"))
            return "megabytes";
        if (desc.matches("(?i).*\\bmilliseconds.*"))
            return "milliseconds";
        if (desc.matches("(?i).*\\bminutes.*"))
            return "minutes";
        if (desc.matches("(?i).*\\bseconds.*"))
            return "seconds";
        if (desc.matches("(?i).*\\bbytes.*"))
            return "bytes";
        if (desc.matches("(?i).*\\bpercent.*"))
            return "percentage";
        return null;
    }

}
