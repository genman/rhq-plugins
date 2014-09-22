package com.apple.iad.rhq.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.testng.annotations.Test;

public class DFSPerformanceTest {

    @Test
    public void test() throws IOException {
        Configuration config = new Configuration();
        config.set("fs.default.name","hdfs://localhost:8020/");
        FileSystem fs = FileSystem.get(config);
        DFSPerformance dfs = new DFSPerformance(fs, "/tmp/test", 10, 1024 * 1024);
        long test = dfs.test();
        System.out.println("comleted in " + test);
        fs.close();
    }
}
