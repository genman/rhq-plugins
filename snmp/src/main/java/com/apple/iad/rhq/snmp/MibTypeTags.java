package com.apple.iad.rhq.snmp;

import java.util.HashSet;
import java.util.Set;

import net.percederberg.mibble.MibTypeTag;

/**
 * Various MIB type tags; syntax definitions.
 */
class MibTypeTags {

    /**
     * TODO
     */
    public static final MibTypeTag IpAddress = new MibTypeTag(MibTypeTag.APPLICATION_CATEGORY, 0);

    public static final MibTypeTag Counter = new MibTypeTag(MibTypeTag.APPLICATION_CATEGORY, 1);
    public static final MibTypeTag Gauge = new MibTypeTag(MibTypeTag.APPLICATION_CATEGORY, 2);
    public static final MibTypeTag TimeTicks = new MibTypeTag(MibTypeTag.APPLICATION_CATEGORY, 3);

    /**
     * TODO
     */
    public static final MibTypeTag Opaque = new MibTypeTag(MibTypeTag.APPLICATION_CATEGORY, 3);

    private static Set<MibTypeTag> measurements = new HashSet<MibTypeTag>();
    static {
        measurements.add(Counter);
        measurements.add(Gauge);
        measurements.add(TimeTicks);
        measurements.add(MibTypeTag.INTEGER);
        measurements.add(MibTypeTag.REAL);
    }

    public static boolean isMeasurement(MibTypeTag tag) {
        return measurements.contains(tag);
    }

}
