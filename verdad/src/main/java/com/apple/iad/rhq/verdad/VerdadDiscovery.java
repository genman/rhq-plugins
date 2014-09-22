package com.apple.iad.rhq.verdad;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

/**
 * Discovers 'vd' executable.
 */
public class VerdadDiscovery implements ResourceDiscoveryComponent<VerdadComponent> {

    private static Log log = LogFactory.getLog(VerdadDiscovery.class);
    private static final Pattern p = Pattern.compile("vd (\\S+) ");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<VerdadComponent> context)
            throws InvalidPluginConfigurationException, Exception {
        log.debug("discover");
        Set<DiscoveredResourceDetails> drds = new HashSet<DiscoveredResourceDetails>();
        try {
            String ver = discover(context);
            if (ver == null)
                return drds;
            Matcher m = p.matcher(ver);
            if (m.find())
                ver = m.group(1);
            else
                log.warn("no version " + ver);
            DiscoveredResourceDetails drd = new DiscoveredResourceDetails(context.getResourceType(),
                    "vd",
                    "Verdad",
                    ver, "Verdad Client", context.getDefaultPluginConfiguration(),
                    null);
            drds.add(drd);
            log.debug("found verdad " + drd);
        } catch (Exception e) {
            log.error("could not run verdad", e);
        }
        return drds;
    }

    /**
     * Returns the version of verdad; or null if the executable was not found.
     */
    String discover(ResourceDiscoveryContext context) throws Exception {
        File f = new File(VerdadComponent.getVD(context.getDefaultPluginConfiguration()));
        if (!f.canExecute()) {
            log.debug("cannot execute " + f);
            return null;
        }
        ProcessExecution pe = new ProcessExecution(f.toString());
        pe.setArguments(new String[]{ "--version" });
        pe.setKillOnTimeout(true);
        pe.setWaitForCompletion(5000); // sometimes slow to get version info
        pe.setCaptureOutput(true);
        ProcessExecutionResults res = context.getSystemInformation().executeProcess(pe);
        if (res.getError() != null)
            throw new Exception(res.getError());
        return res.getCapturedOutput();
    }

}
