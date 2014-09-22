package com.apple.iad.rhq.datatorrent;

import static java.lang.Integer.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import com.apple.iad.rhq.http.HttpComponent;

/**
 * Top-level component representing the Gateway.
 */
public class GWComponent extends HttpComponent<ResourceComponent<?>> {

    private final String FINDPORT = "-findport";

    // override port
    private int port = 0;

    // override host
    private String host;

    public GWComponent() {
    }

    /**
     * This is used by discovery to figure out the URL.
     */
    public GWComponent(Configuration configuration, ResourceDiscoveryContext<?> context) throws IOException {
        super(configuration, context);
        for (ProcessScanResult psr: context.getAutoDiscoveredProcesses()) {
            ProcessInfo pi = psr.getProcessInfo();
            log.debug("found " + pi);
            discovered(pi.getCommandLine());
        }
    }

    void discovered(String[] commandLine) throws IOException {
        boolean fp = false;
        File fpf = null;
        for (String arg : commandLine) {
            if (fp) {
                fpf = new File(arg);
                break;
            }
            if (arg.equals(FINDPORT)) {
                fp = true;
            }
        }
        if (fpf != null) {
            if (fpf.canRead()) {
                FileReader fr = new FileReader(fpf);
                String addr;
                try {
                    addr = new BufferedReader(fr).readLine();
                } finally {
                    fr.close();
                }
                if (addr != null) {
                    log.debug("discovered " + addr);
                    String s[] = addr.split(":");
                    host = s[0];
                    port = parseInt(s[1]);
                }
            } else {
                log.warn("cannot open " + fpf);
            }
        } else {
            log.debug("no arg " + FINDPORT);
        }
    }

    @Override
    public URL getUrl() throws IOException {
        URL url = super.getUrl();
        if (host != null) {
            // discovered host/port
            return new URL(url.getProtocol(), host, port, url.getFile());
        }
        return url;
    }

    @Override
    protected String getVersion() {
        String ver;
        try {
            ver = (String) getMeasurementProvider().extractValue("version");
            return AppDiscovery.getVersion(ver);
        } catch (Exception e) {
            return null;
        }
    }

}
