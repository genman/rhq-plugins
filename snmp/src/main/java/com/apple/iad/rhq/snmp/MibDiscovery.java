package com.apple.iad.rhq.snmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

/**
 * Loads a MIB and pulls data from an SNMP connection.
 */
public class MibDiscovery implements ResourceDiscoveryComponent<SnmpComponentHolder>, ManualAddFacet<ResourceComponent<?>> {

    /**
     * Log handler.
     */
    protected Log log = LogFactory.getLog(getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<SnmpComponentHolder> rdc) throws Exception {

        Set<DiscoveredResourceDetails> drd = new HashSet<DiscoveredResourceDetails>();
        String rtName = rdc.getResourceType().getName();
        log.debug("discover " + rtName);

        Set<MeasurementDefinition> mds = rdc.getResourceType().getMetricDefinitions();
        List<OID> oids = new ArrayList<OID>();
        Configuration conf = rdc.getDefaultPluginConfiguration();
        MibIndex digest = MibComponent.index(conf);
        for (MeasurementDefinition md : mds) {
            OID oid = digest.getNameRecord(md.getName()).getOid0();
            oids.add(oid);
        }
        SnmpComponent snmp = rdc.getParentResourceComponent().getSnmpComponent();
        if (oids.size() > 3) {
            // don't discover entire OID list
            oids = oids.subList(0, 3);
        }
        Map<OID, Variable> map = snmp.get(oids);
        if (map.isEmpty()) {
            log.debug("could not discover " + rtName + "; map empty");
            return drd;
        }
        for (Variable v : map.values()) {
            if (v.isException()) {
                log.debug("could not discover " + rtName + "; variable was " + v);
                return drd;
            }
        }

        String version = null;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(), // ResourceType
                rtName, // won't run multiple instances (probably)
                rtName, // resource name
                version, // Version
                rtName, // unique name
                conf,
                null // process information
        );

        drd.add(detail);
        return drd;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration conf,
            ResourceDiscoveryContext<ResourceComponent<?>> rdc)
            throws InvalidPluginConfigurationException {
        String rtName = rdc.getResourceType().getName();
        String version = null;
        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                rdc.getResourceType(), // ResourceType
                rtName,
                rtName,
                version, // Version
                rtName,
                conf,
                null // process information
        );

        return detail;
    }


}
