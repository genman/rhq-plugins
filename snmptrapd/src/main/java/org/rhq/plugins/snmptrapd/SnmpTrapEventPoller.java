/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.snmptrapd;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.pluginapi.event.EventContext;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import com.apple.iad.rhq.snmp.MibIndex;

/**
 * Polls the individual traps
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapEventPoller implements CommandResponder {

    private final Log log = LogFactory.getLog(SnmpTrapEventPoller.class);

    private final MibIndex index;
    private final AtomicInteger count = new AtomicInteger();
    private final Rules rules;
    private final OID sourceOid;
    private final Set<OID> omits;

    private final EventContext eventContext;

    private final Pattern filter;

    private final Charset charset;

    /**
     * Constructs a poller.
     */
    public SnmpTrapEventPoller(MibIndex index, Rules rules, OID sourceOid, Set<OID> omits, EventContext eventContext, Pattern filter, Charset charset) {
        this.index = index;
        this.rules = rules;
        this.sourceOid = sourceOid;
        this.omits = omits;
        this.eventContext = eventContext;
        this.filter = filter;
        this.charset = charset;
    }

    /**
     * Callback from the SNMP library. Will be called on each incoming trap.
     */
    public void processPdu(CommandResponderEvent cre) {
        if (log.isDebugEnabled())
            log.debug("recv: " + cre);
        PDU pdu = cre.getPDU();
        String sourceAddr;
        Address addr = cre.getPeerAddress();
        if (addr instanceof IpAddress) {
            InetAddress inet = ((IpAddress) addr).getInetAddress();
            sourceAddr = inet.getHostName();
            if (sourceAddr == null) {
                sourceAddr = inet.getHostAddress();
                if (sourceAddr == null) { // shouldn't happen
                    sourceAddr = "unknown";
                }
            }
        } else {
            // Don't use addr.toString() as this would contain the port and generate too many
            // EventSources
            sourceAddr = "snmp-agent";
        }
        if (pdu != null) {
            StringBuilder payload = new StringBuilder();
            if (pdu instanceof PDUv1) {
                // SNMP v1
                PDUv1 v1pdu = (PDUv1) pdu;
                long timeTicks = v1pdu.getTimestamp();
                payload.append("Traptype (generic, specific): ");
                payload.append(v1pdu.getGenericTrap()).append(", ").append(v1pdu.getSpecificTrap()).append("\n");
                payload.append("Timestamp: ").append(new TimeTicks(timeTicks)).append("\n");
                OID oid = v1pdu.getEnterprise();
                payload.append("Enterprise: ").append(index.toName(oid)).append("\n");
            }

            count.getAndIncrement();

            EventSeverity severity = rules.match(pdu);

            List<? extends VariableBinding> vbs = pdu.getVariableBindings();
            for (VariableBinding vb : vbs) {
                OID oid = vb.getOid();
                if (oid.last() == 0)
                    oid.removeLast();
                if (omits.contains(oid))
                    continue;
                if (oid.equals(sourceOid)) {
                    Variable v = pdu.getVariable(sourceOid);
                    if (v != null)
                        sourceAddr = v.toString();
                    // see Bugzilla 911432
                    // continue;
                }
                Object var = vb.getVariable();
                if (var instanceof OID) {
                    var = index.toName((OID)var);
                }
                if (var instanceof OctetString) {
                    // if the string contains non-ASCII, OctetString.toString shows HEX
                    var = toString((OctetString)var);
                }
                String name = index.toName(oid);
                if (var instanceof UnsignedInteger32) {
                    int i = ((UnsignedInteger32)var).toInt();
                    Map<Integer, String> mapping = index.getMapping(name);
                    if (mapping != null) {
                        var = mapping.get(i);
                    }
                }
                payload.append(name).append(": ").append(var).append("\n");
            }

            String filtered = filter.matcher(payload).replaceAll("");
            String type = SnmpTrapdComponent.TRAP_TYPE;
            Event event = new Event(type, sourceAddr, System.currentTimeMillis(), severity, filtered);
            if (log.isDebugEnabled())
                log.debug("queue event " + event);

            eventContext.publishEvent(event);

        }
    }

    private String toString(OctetString s) {
        return new String(s.getValue(), charset);
    }

    /**
     * Returns the number of traps.
     */
    public int getTrapCount() {
        return count.get();
    }

}
