package com.apple.iad.rhq.hadoop;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import com.apple.iad.rhq.hadoop.HadoopClassLoaderFacet;

/**
 * Facet that locates hadoop-core.jar; scanning the usual places.
 */
public class HadoopClassLoaderFacet implements ClassLoaderFacet {

    private static Log log = LogFactory.getLog(HadoopClassLoaderFacet.class);

    private static final Pattern home = Pattern.compile("-Dhadoop.home.dir=(\\S+)");

    /**
     * Core jar name.
     */
    public static final String core_jar = "hadoop-core";

    /**
     * Core jar name.
     */
    public static final Pattern lib_jar = Pattern.compile("guava.*");

    /**
     * Default Hadoop home directory.
     */
    public static final String hadoop_home = "/usr/lib/hadoop";

    /**
     * Plugin configuration key.
     */
    public static final String hadoop_home_key = "hadoop.home.dir";

    /**
     * Scans hadoop.home.dir as a config key for {@link #core_jar}.
     */
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext rdc, DiscoveredResourceDetails drd)
            throws Exception
    {
        return getURL(drd);
    }

    private String getHome(String[] commandLine) {
        for (String line : commandLine) {
            Matcher m = home.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private List<URL> scan(File dir) throws MalformedURLException {
        log.info("scan " + dir);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(core_jar);
            }
        });
        List<URL> urls = new ArrayList<URL>();
        append(files, urls);
        File lib = new File(dir, "lib");
        files = lib.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return lib_jar.matcher(name).matches();
            }
        });
        append(files, urls);
        log.info("built hadoop classpath " + urls);
        return urls;
    }

    void append(File[] files, List<URL> urls) throws MalformedURLException {
        for (File f : files)
            urls.add(f.toURI().toURL());
    }

    private List<URL> getURL(DiscoveredResourceDetails drd) throws MalformedURLException {
        ProcessInfo pi = drd.getProcessInfo();
        if (pi != null) {
            // This currently isn't supported by RHQ anyway
            String home = getHome(pi.getCommandLine());
            return scan(new File(home));
        }
        String home_str = drd.getPluginConfiguration().getSimpleValue(hadoop_home_key, hadoop_home);
        File home = new File(home_str);
        if (!home.exists()) {
            log.warn("cannot find home " + home);
            return Collections.emptyList();
        }
        return scan(home);
    }

}
