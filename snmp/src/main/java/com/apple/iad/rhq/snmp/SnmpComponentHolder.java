package com.apple.iad.rhq.snmp;

import org.rhq.core.pluginapi.inventory.ResourceComponent;


/**
 * Interface for component which holds a reference to an SnmpComponent.
 */
public interface SnmpComponentHolder extends ResourceComponent<SnmpComponentHolder> {

    /**
     * Returns the SNMP component.
     */
    SnmpComponent getSnmpComponent();

}
