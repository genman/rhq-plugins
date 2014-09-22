package com.apple.iad.rhq.hadoop;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

public class DiscoveryVersionTest {

    static String[] split(String s) {
        return s.split("\\s");
    }

    String s[] = split("/usr/java/jdk1.6.0_17/bin/java -Dproc_balancer -Xmx1000m -Xmx4g -Dcom.sun.management.jmxremote -Xmx4g -Dcom.sun.management.jmxremote -Xmx4g -Dcom.sun.management.jmxremote -Xmx4g -Dcom.sun.management.jmxremote -Xmx4g -Dcom.sun.management.jmxremote -Dhadoop.log.dir=/data/hlog/logs -Dhadoop.log.file=hadoop-hdfs-balancer-st11p01ad-qc6001.apple.com.log -Dhadoop.home.dir=/usr/lib/hadoop/bin/.. -Dhadoop.id.str=hdfs -Dhadoop.root.logger=INFO,DRFA -Djava.library.path=/usr/lib/hadoop/bin/../lib/native/Linux-amd64-64 -Dhadoop.policy.file=hadoop-policy.xml -classpath /usr/lib/hadoop/bin/../conf:/usr/java/jdk1.6.0_17/lib/tools.jar:/usr/lib/hadoop/bin/..:" +
            "/usr/lib/hadoop/bin/../hadoop-core-0.20.2+737.jar:/usr/lib/hadoop/bin/../lib/aspectjrt-1.6.5.jar:/usr/lib/hadoop/bin/../lib/aspectjtools-1.6.5.jar:/usr/lib/hadoop/bin/../lib/commons-cli-1.2.jar:/usr/lib/hadoop/bin/../lib/commons-codec-1.4.jar:/usr/lib/hadoop/bin/../lib/commons-daemon-1.0.1.jar:/usr/lib/hadoop/bin/../lib/commons-el-1.0.jar:/usr/lib/hadoop/bin/../lib/commons-httpclient-3.0.1.jar:/usr/lib/hadoop/bin/../lib/commons-logging-1.0.4.jar:/usr/lib/hadoop/bin/../lib/commons-logging-api-1.0.4.jar:/usr/lib/hadoop/bin/../lib/commons-net-1.4.1.jar:/usr/lib/hadoop/bin/../lib/core-3.1.1.jar:/usr/lib/hadoop/bin/../lib/hadoop-fairscheduler-0.20.2+737.jar:/usr/lib/hadoop/bin/../lib/hsqldb-1.8.0.10.jar:/usr/lib/hadoop/bin/../lib/hue-plugins-1.1.0.jar:/usr/lib/hadoop/bin/../lib/jackson-core-asl-1.5.2.jar:/usr/lib/hadoop/bin/../lib/jackson-mapper-asl-1.5.2.jar:/usr/lib/hadoop/bin/../lib/jasper-compiler-5.5.12.jar:/usr/lib/hadoop/bin/../lib/jasper-runtime-5.5.12.jar:/usr/lib/hadoop/bin/../lib/jets3t-0.6.1.jar:/usr/lib/hadoop/bin/../lib/jetty-6.1.14.jar:/usr/lib/hadoop/bin/../lib/jetty-util-6.1.14.jar:/usr/lib/hadoop/bin/../lib/junit-4.5.jar:/usr/lib/hadoop/bin/../lib/kfs-0.2.2.jar:/usr/lib/hadoop/bin/../lib/log4j-1.2.15.jar:/usr/lib/hadoop/bin/../lib/mockito-all-1.8.2.jar:" +
    "/usr/lib/hadoop/bin/../lib/mysql-connector-java-5.0.8-bin.jar:/usr/lib/hadoop/bin/../lib/oro-2.0.8.jar:/usr/lib/hadoop/bin/../lib/servlet-api-2.5-6.1.14.jar:/usr/lib/hadoop/bin/../lib/slf4j-api-1.4.3.jar:/usr/lib/hadoop/bin/../lib/slf4j-log4j12-1.4.3.jar:/usr/lib/hadoop/bin/../lib/toddlipcon-hadoop-lzo-20100528142758.20100528133131.3ebdc92.jar:/usr/lib/hadoop/bin/../lib/xmlenc-0.52.jar:/usr/lib/hadoop/bin/../lib/jsp-2.1/jsp-2.1.jar:/usr/lib/hadoop/bin/../lib/jsp-2.1/jsp-api-2.1.jar org.apache.hadoop.hdfs.server.balancer.Balancer");

    String s2[] = split("java hbase-0.90.6-cdh3u4.jar");

    @Test
    public void test() {
        HadoopDiscovery hd = new HadoopDiscovery();
        String version = hd.getVersion(s);
        assertEquals("0.20.2+737", version);

        version = hd.getVersion(s2);
        assertEquals("0.90.6-cdh3u4", version);
    }

}
