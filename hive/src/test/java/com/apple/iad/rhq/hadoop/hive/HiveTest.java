package com.apple.iad.rhq.hadoop.hive;

import static org.testng.AssertJUnit.assertEquals;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.ResourceType;
import org.testng.annotations.Test;

import com.apple.iad.rhq.hadoop.hive.HiveServerComponent;
import com.apple.iad.rhq.hadoop.hive.HiveTable;
import com.apple.iad.rhq.testing.ComponentTest;

@Test
public class HiveTest extends ComponentTest {

    private static final String name = "HiveServer";
    HiveServerComponent hive;

    @Override
    protected void before() throws Exception {
        String path = "hdfs://hostname/data/iad/stg_uuid_cmp/period=daily/dt=2011-07-14/part=requests";
        assertEquals("/data/iad/stg_uuid_cmp", HiveTable.fixPath(path));

        path = "hdfs://hostname/data/iad/stg_uuid_cmp/period=daily and not so daily";
        assertEquals("/data/iad/stg_uuid_cmp", HiveTable.fixPath(path));

        assertEquals("/data/iad/stg_batch_txn_log", HiveTable.fixPath("/data/iad/stg_batch_txn_log/2011-11-04/201111040900"));
        assertEquals("/data/iad/iad_iadid_bot_stats_t", HiveTable.fixPath("/data/iad/iad_iadid_bot_stats_t/dt=$%7BDATE1}"));

        setProcessScan(false);
        super.before();
        ResourceType resourceType = getResourceType(name);
        Configuration configuration = getConfiguration(resourceType);
        set(configuration, "fs.default.name", "hdfs://vp25q03ad-hadoop091.iad.apple.com:8020/");
        set(configuration, "jdbc.url", "jdbc:mysql://vp25q03ad-hadoopfeeder011:3306/metastore_iadse?user=hive&password=hive");
        set(configuration, "table.match", "stg_uuid_cmp.*");
        hive = (HiveServerComponent) manuallyAdd(resourceType, configuration);
    }

    public void test() throws Exception {
        assertUp(hive);
        HiveTable ht = (HiveTable)getComponent("stg_uuid_cmp_conn");
        MeasurementReport mr = getMeasurementReport(ht);
        log.info("report " + mr);
        assertAll(mr, this.getResourceDescriptor("HiveTable"));
    }


}
