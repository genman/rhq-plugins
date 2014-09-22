package com.apple.iad.rhq.snmp;

import static java.lang.String.valueOf;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.AssignableFromByteArray;
import org.snmp4j.smi.AssignableFromLong;
import org.snmp4j.smi.AssignableFromString;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

/**
 * Performs the 'set' operation.
 */
public class SetOperation {

    private static final String GET = "GET";

    /**
     * Format parameter.
     */
    public static final String FORMAT = "format";

    /**
     * Value parameter.
     */
    public static final String VALUE = "value";

    /**
     * OID parameter.
     */
    public static final String OID = "oid";

    /**
     * Return status.
     */
    public static final String ERROR_STATUS_TEXT = "errorStatusText";

    /**
     * Return status.
     */
    public static final String ERROR_STATUS = "errorStatus";

    /**
     * Return status.
     */
    public static final String ERROR_INDEX = "errorIndex";

    /**
     * Log handle.
     */
    protected final Log log = LogFactory.getLog(getClass());

    private final SnmpComponent component;

    private final OID row;

    /**
     * Constructs with a connector.
     */
    public SetOperation(SnmpComponent component) {
        this(component, null);
    }

    /**
     * Constructs with a connector and table row.
     */
    public SetOperation(SnmpComponent component, OID row) {
        this.component = component;
        this.row = row;
    }

    /**
     * Performs the set operation from the RHQ UI.
     */
    public OperationResult set(Configuration parameters) throws Exception {
        String oidStr = parameters.getSimpleValue(OID, "");
        String value = parameters.getSimpleValue(VALUE, "");
        String format = parameters.getSimpleValue(FORMAT, GET);
        MibIndex index = MibIndexCache.getIndex();
        OID oid = index.toOid(oidStr);
        if (row != null)
            oid = new OID(oid.getValue(), row.getValue());
        else if (oid.last() != 0)
            oid.append(0);
        Variable variable = createVariable(value, format, oid);
        VariableBinding vb = new VariableBinding(oid, variable);
        ResponseEvent event = component.set(vb);
        return getResponse(event);
    }

    private OperationResult getResponse(ResponseEvent event) {
        PDU response = event.getResponse();
        log.info("set result " + response);
        int errorIndex = response.getErrorIndex();
        int errorStatus = response.getErrorStatus();
        String errorStatusText = response.getErrorStatusText();
        OperationResult result = new OperationResult();
        Configuration config = result.getComplexResults();
        config.setSimpleValue(ERROR_INDEX, valueOf(errorIndex));
        config.setSimpleValue(ERROR_STATUS, valueOf(errorStatus));
        config.setSimpleValue(ERROR_STATUS_TEXT, valueOf(errorStatusText));
        if (errorStatus != SnmpConstants.SNMP_ERROR_SUCCESS)
            result.setErrorMessage(response.toString());
        return result;
    }

    private Variable createVariable(String value, String format, org.snmp4j.smi.OID oid) throws Exception {
        byte ba[] = null;
        if (value.startsWith("0x")) {
            value = value.substring(2);
            ba = DatatypeConverter.parseHexBinary(value);
        }
        Variable variable;
        if (format.equals(GET)) {
            log.debug("obtaining the existing variable type");
            variable = component.get(oid);
        } else {
            try {
                variable = (Variable) Class.forName("org.snmp4j.smi." + format).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("bad format " + format, e);
            }
        }
        if (variable instanceof AssignableFromLong) {
            log.debug("assign as long");
            ((AssignableFromLong)variable).setValue(Long.valueOf(value));
        } else if (variable instanceof AssignableFromByteArray && ba != null) {
            log.debug("assign as byte array");
            ((AssignableFromByteArray)variable).setValue(ba);
        } else if (variable instanceof AssignableFromString) {
            log.debug("assign as string");
            ((AssignableFromString)variable).setValue(value);
        } else {
            throw new IllegalStateException("cannot assign value");
        }
        return variable;
    }


}
