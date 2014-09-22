package org.rhq.plugins.snmptrapd;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.EventSeverity;
import org.snmp4j.PDU;

import com.apple.iad.rhq.snmp.MibIndex;


/**
 * Rules for a severity level.
 */
public class Rules {

    private final List<Rule> rules;

    private Rules(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Return a matching event severity, or INFO if not matched.
     */
    public EventSeverity match(PDU pdu) {
        for (Rule rule : rules) {
            EventSeverity es = rule.match(pdu);
            if (es != null)
                return es;
        }
        return EventSeverity.INFO;
    }

    /**
     * Rules from a property list.
     * @param index Mib Index
     * @param list may be null
     */
    public static Rules rules(MibIndex index, PropertyList list) {
        List<Rule> rules = new ArrayList<Rule>();
        if (list != null) {
            for (Property property : list.getList()) {
                rules.add(new Rule(index, (PropertyMap)property));
            }
        }
        return new Rules(rules);
    }

    /**
     * Debug string.
     */
    @Override
    public String toString() {
        return "Rules [rules=" + rules + "]";
    }

}
