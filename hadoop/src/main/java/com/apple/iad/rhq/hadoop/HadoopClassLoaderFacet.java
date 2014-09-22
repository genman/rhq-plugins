package com.apple.iad.rhq.hadoop;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;

import com.apple.iad.rhq.hadoop.HadoopClassLoaderFacet;

/**
 * Facet that locates hadoop classfiles.
 */
public class HadoopClassLoaderFacet implements ClassLoaderFacet {

    public static final String HADOOP_CLASSPATH_COMMAND = "hadoop.classpath.command";

    static final String DEFAULT_COMMAND = "/usr/bin/hadoop classpath";

    private static Log log = LogFactory.getLog(HadoopClassLoaderFacet.class);

    /**
     * Obtains the classpath calling a command.
     */
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        return getURL(rdc, drd);
    }

    private List<URL> scan(List<URL> urls, File dir) throws MalformedURLException {
        log.debug("scan " + dir);
        if (!dir.isDirectory()) {
            log.warn("not directory " + dir);
        }
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                return f.isFile() && f.canRead();
            }
        });
        append(files, urls);
        return urls;
    }

    void append(File[] files, List<URL> urls) throws MalformedURLException {
        for (File f : files) {
            URL url = f.toURI().toURL();
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
    }

    private List<URL> getURL(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd) throws IOException {
        ProcessInfo pi = drd.getProcessInfo();
        if (pi != null) {
            // This currently isn't supported by RHQ anyway
            // String home = getHome(pi.getCommandLine());
            // return scan(new File(home));
        }
        String command = drd.getPluginConfiguration().getSimpleValue(HADOOP_CLASSPATH_COMMAND, DEFAULT_COMMAND);
        String[] split = command.split("\\s+");
        List<String> splitL = asList(split);
        ProcessExecution pe = new ProcessExecution(splitL.get(0));
        pe.setWaitForCompletion(1000 * 60);
        pe.setArguments(splitL.subList(1, splitL.size()));
        pe.setCaptureOutput(true);
        pe.isCheckExecutableExists();
        ProcessExecutionResults result = rdc.getSystemInformation().executeProcess(pe);
        if (result.getError() != null)
            throw new RuntimeException(result.getError());
        String cp = result.getCapturedOutput();

        log.debug("classpath is " + cp);
        List<URL> urls = new ArrayList<URL>();
        String[] dirs = cp.split(":");
        for (String dir : dirs) {
            dir = dir.trim();
            if (dir.endsWith("*")) {
                dir = dir.substring(0, dir.length() - 1);
                File f = new File(dir);
                scan(urls, f.getCanonicalFile());
            } else {
                File f = new File(dir);
                urls.add(f.getCanonicalFile().toURI().toURL());
            }
        }
        log.debug("built hadoop classpath " + urls);

        return urls;
    }

}
