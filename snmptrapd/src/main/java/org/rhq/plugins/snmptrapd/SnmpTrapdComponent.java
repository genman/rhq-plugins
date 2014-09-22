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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.exception.ThrowableUtil;
import org.snmp4j.Snmp;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.apple.iad.rhq.snmp.MibComponent;
import com.apple.iad.rhq.snmp.MibIndex;
import com.apple.iad.rhq.snmp.MibIndexCache;


/**
 * The actual implementation of the Snmp trapd
 * @author Heiko W. Rupp
 *
 */
public class SnmpTrapdComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    private final Log log = LogFactory.getLog(SnmpTrapdComponent.class);

    public static final String TRAP_TYPE = "SnmpTrap";
    public static final String MIBS = "mibs";
    public static final String SOURCE_LOCATION = "sourceLocation";
    public static final String CHARSET = "charset";

    private EventContext eventContext;
    private Snmp snmp;
    private SnmpTrapEventPoller snmpTrapEventPoller;

    private Rules rules;
    private MibIndex index;


    static {
        org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    /**
     * Start the event polling mechanism and the actual trap listener.
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        int port = Integer.parseInt(conf.getSimpleValue("port", "119"));

        ResourceComponent component = context.getParentResourceComponent();
        if (component instanceof MibComponent) {
            index = ((MibComponent) component).getIndex();
        } else {
            index = MibIndexCache.getIndex();
        }
        String mibs = conf.getSimpleValue(MIBS, "");
        log.debug("mibs " + mibs);
        for (String mib : mibs.split("[, ]+")) {
            if (!mib.isEmpty()) {
                log.debug("load mib " + mib);
                index.load(mib);
            }
        }

        Set<OID> omits = new HashSet<OID>();
        PropertyList omit = conf.getList("omitOids");
        if (omit != null) {
            for (Property property : omit.getList()) {
                String s = ((PropertySimple)property).getStringValue();
                omits.add(index.toOid(s));
            }
        }

        PropertyList list = conf.getList("rules");
        rules = Rules.rules(index, list);
        eventContext = context.getEventContext();

        String regex = conf.getSimpleValue("filter", "\0");
        Pattern filter = Pattern.compile(regex);

        // TODO: check if the engine is already alive
        try {
            UdpAddress targetAddress = new UdpAddress(port);
            snmp = new Snmp(new DefaultUdpTransportMapping());
            String sourceLocation = conf.getSimpleValue(SOURCE_LOCATION, "");
            OID sourceOid = null;
            if (!sourceLocation.isEmpty())
                sourceOid = index.toOid(sourceLocation);
            String charset = conf.getSimpleValue(CHARSET, "UTF-8");
            snmpTrapEventPoller = new SnmpTrapEventPoller(index, rules, sourceOid, omits, eventContext, filter,
                    Charset.forName(charset));
            // TODO set up the community here
            if (!snmp.addNotificationListener(targetAddress, snmpTrapEventPoller))
                throw new IOException("cannot attach to " + targetAddress);
            //transport.listen();
        } catch (IOException e) {
            log.error("Cannot start snmp engine. Cause: " + ThrowableUtil.getAllMessages(e));
        }
    }

    /**
     * Tear down the trap listener and stop polling for events.
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        if (snmp != null) {
            snmp.removeCommandResponder(snmpTrapEventPoller);
            try {
                snmp.close();
            } catch (IOException e) {
                log.error("Cannot stop snmp engine. Cause: " + ThrowableUtil.getAllMessages(e));
            }
            snmp = null;
        }

        eventContext.unregisterEventPoller(TRAP_TYPE);
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("trap_count")) {
                int trapCount = snmpTrapEventPoller.getTrapCount();
                MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(trapCount));
                report.addData(res);
            }
        }
    }

}
