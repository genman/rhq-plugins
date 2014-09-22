package com.apple.iad.rhq.snmp;

import org.snmp4j.agent.security.VACM;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class TestVACM implements VACM {

    @Override
    public int isAccessAllowed(OctetString paramOctetString1,
            OctetString paramOctetString2, int paramInt1, int paramInt2,
            int paramInt3, OID paramOID) {
        return VACM_OK;
    }

    @Override
    public int isAccessAllowed(OctetString paramOctetString, OID paramOID) {
        return VACM_OK;
    }

    @Override
    public OctetString getViewName(OctetString paramOctetString1,
            OctetString paramOctetString2, int paramInt1, int paramInt2,
            int paramInt3) {
        return new OctetString();
    }

}
