package com.apple.iad.rhq.testing;

import java.util.List;

import org.rhq.core.system.NativeSystemInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.pquery.ProcessInfoQuery;

/**
 * Tool to test if a process matches or not.
 * Usage: java -cp rhq-plugin-testing-xxx.jar:/opt/rhq/rhq-agent/lib/* org.rhq.plugins.testing.ProcessQuery
 */
public class ProcessQuery {

    /**
     * Process arguments.
     */
    public static void main(String s[]) {
        println("Usage: java " + ProcessQuery.class + " <piql0> <piql1> ... ");
        List<ProcessInfo> pi = new NativeSystemInfo().getAllProcesses();
        for (String piq : s) {
            ProcessInfoQuery piql = new ProcessInfoQuery(pi);
            println("PIQL " + piq);
            for (ProcessInfo info : piql.query(piq)) {
                println("match " + info);
            }
        }
    }

    private static void println(String string) {
        System.out.println(string);
    }

}
