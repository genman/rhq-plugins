package org.rhq.plugins.snmptrapd;

import static java.lang.Integer.parseInt;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.event.EventSeverity;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import com.apple.iad.rhq.snmp.MibIndex;


/**
 * Severity matching rule for a SNMP PDU.
 */
public class Rule {

    private static final Log log = LogFactory.getLog(Rule.class);

    /**
     * Field is unset.
     */
    public static final int UNSET = -1;

    private int genericTrap = UNSET;
    private int specificTrap = UNSET;
    private final OID enterprise;
    private final OID trapOid;
    private final OID varbind;
    private final Pattern value;
    private final EventSeverity severity;

    /**
     * Constructs an arbitrary rule.
     *
     * @param genericTrap
     * @param specificTrap
     * @param enterprise
     * @param trapOid
     * @param varbind
     * @param value
     * @param severity
     */
    public Rule(int genericTrap, int specificTrap, OID enterprise, OID trapOid,
            OID varbind, Pattern value, EventSeverity severity) {
        if (enterprise == null)
            throw new NullPointerException();
        if (trapOid == null)
            throw new NullPointerException();
        if (varbind == null)
            throw new NullPointerException();
        if (value == null)
            throw new NullPointerException();
        if (severity == null)
            throw new NullPointerException();
        this.genericTrap = genericTrap;
        this.specificTrap = specificTrap;
        this.enterprise = enterprise;
        trapOid = trapOid(genericTrap, specificTrap, trapOid);
        this.trapOid = trapOid;
        this.varbind = varbind;
        this.value = value;
        this.severity = severity;
    }

    private OID trapOid(int genericTrap, int specificTrap, OID trapOid) {
        if (trapOid.size() == 0 && genericTrap != UNSET) {
            return SnmpConstants.getTrapOID(SnmpConstants.snmpTraps, genericTrap, specificTrap);
        }
        return trapOid;
    }

    /**
     * Constructs a new Rule.
     * @param index MibIndex to reference
     * @param p RHQ configuration.
     */
    public Rule(MibIndex index, PropertyMap p) {
        genericTrap = parseInt(p.getSimpleValue("genericTrap", "-1"));
        specificTrap = parseInt(p.getSimpleValue("specificTrap", "-1"));
        String e = p.getSimpleValue("enterprise", "");
        enterprise = index.toOid(e);
        String t = p.getSimpleValue("trapOid", "");
        OID trapOid = index.toOid(t);
        String b = p.getSimpleValue("varbind", "");
        varbind = index.toOid(b);
        String v = p.getSimpleValue("value", "");
        try {
            value = Pattern.compile(v);
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("'value' needs to be a regular expression: " + ex);
        }
        trapOid = trapOid(genericTrap, specificTrap, trapOid);
        severity = EventSeverity.valueOf(p.getSimpleValue("severity", "INFO").toUpperCase(Locale.ENGLISH));
        this.trapOid = trapOid;
    }

    /**
     * Returns the matching severity for this PDU, or null if no match.
     */
    public EventSeverity match(PDU pdu) {
        if (pdu instanceof PDUv1) {
            PDUv1 pdu1 = (PDUv1) pdu;
            if (genericTrap != UNSET && genericTrap != pdu1.getGenericTrap()) {
                log.trace("no match generic trap");
                return null;
            }
            if (specificTrap != UNSET && specificTrap != pdu1.getSpecificTrap()) {
                log.trace("no match specific trap");
                return null;
            }
            if (!pdu1.getEnterprise().startsWith(enterprise)) {
                log.trace("no match enterprise");
                return null;
            }
        } else {
            OID oid = (OID) pdu.getVariable(SnmpConstants.snmpTrapOID);
            if (oid != null && oid.startsWith(trapOid)) {
                log.trace("no match enterprise");
                return null;
            }
        }
        if (varbind.size() != 0) {
            Variable v = pdu.getVariable(varbind);
            if (v == null) {
                log.trace("no required variable found");
                return null;
            }
            if (!value.matcher(v.toString()).find()) {
                log.trace("no match variable");
                return null;
            }
        }
        return severity;
    }

    @Override
    public String toString() {
        return "Rule [genericTrap=" + genericTrap + ", specificTrap="
                + specificTrap + ", enterprise=" + enterprise + ", trapOid="
                + trapOid + ", varbind=" + varbind + ", value=" + value.pattern()
                + ", severity=" + severity + "]";
    }

}
