package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.Map;

import net.percederberg.mibble.MibLoaderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;
import org.testng.annotations.Test;

import com.apple.iad.rhq.snmp.MibIndex;
import com.apple.iad.rhq.snmp.NameRecord;
import com.apple.iad.rhq.snmp.TableRecord;

public class NetscalerIndexTest {

    private Log log = LogFactory.getLog(getClass());

    public final static String NS = "NS-MIB-smiv1.mib";

    @Test
    public void test() throws IOException, MibLoaderException {
        MibIndex mibIndex = new MibIndex();
        mibIndex.load(NS);

        TableRecord tr = mibIndex.getTableRecord("vserverTable");
        OID oid = new OID("11.118.103.54.45.105.97.100.49.45.97.100");
        Map<String, Object> index = tr.index(oid);
        log.info(index);

        oid = new OID("11.118.103.54.45.105.97.100.49.45.97.100");
        index = tr.index(oid);
        log.info(index);

        NameRecord nameRecord;
        nameRecord = mibIndex.getNameRecord("serviceScpolicyTable");
        log.info("table " + nameRecord);
        nameRecord = mibIndex.getNameRecord("serviceScpolicyEntry");
        log.info("entry " + nameRecord);
        //                                 1.3.6.1.4.1.5951.4.1.2.3.1
        log.info(mibIndex.getName(new OID("1.3.6.1.4.1.5951.4.1.2.3.1")));

        tr = mibIndex.getTableRecord("nsIpAddrTable");
        tr = mibIndex.getTableRecord("monServiceMemberTable");
    }

}
