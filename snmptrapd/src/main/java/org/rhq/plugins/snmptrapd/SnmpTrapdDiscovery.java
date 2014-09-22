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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import com.apple.iad.rhq.snmp.MibComponent;


/**
 * Discovery component for an SNMP trapd.
 *
 * The component can be deployed on its own or 'discovered' as child of a {@link MibComponent}.
 *
 * @author Heiko W. Rupp
 */
public class SnmpTrapdDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private final Log log = LogFactory.getLog(SnmpTrapdDiscovery.class);

    private static final String PORT_PROPERTY = "port";

    /**
     * Supports auto-discovery within a {@link MibComponent}.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        ResourceComponent parent = context.getParentResourceComponent();
        if (parent instanceof MibComponent) {
            Configuration configuration = context.getDefaultPluginConfiguration();
            String port = configuration.getSimpleValue(PORT_PROPERTY, "");
            String key = key(port);
            String name = name(port);
            String description = desc(port);

            ResourceType resourceType = context.getResourceType();
            DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
                configuration, null);
            set.add(details);
            log.debug("discovered; details " + details);
        }

        return set;
    }

    private String key(String port) {
        return port;
    }

    private String desc(String port) {
        return "SNMP trap and notification receiver on port " + port;
    }

    private String name(String port) {
        return "SNMP recv " + port;
    }

    /**
     * Manual discovery
     */
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
                                                      ResourceDiscoveryContext ctx) throws InvalidPluginConfigurationException {

        String port = pluginConfiguration.getSimpleValue(PORT_PROPERTY, null);
        String key = key(port);
        String name = name(port);
        String description = desc(port);
        ResourceType resourceType = ctx.getResourceType();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, key, name, null, description,
            pluginConfiguration, null);

        return details;
    }

}
