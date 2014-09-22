package com.apple.iad.rhq.verdad;

import java.util.Map;

/**
 * Implemented by components that are interested in Verdad information.
 */
public interface VerdadObserver {

    /**
     * Observes information from Verdad about this host.
     * The tree is returned from parsing the <code>json2_tree</code> format
     * when reading data from Verdad, which appears as such:
     *
     * <pre>
     * {
     *    "vp25q03ad-hadoop086.iad.apple.com" : {
     * </pre>
     *
     * Note this method may be called even if the document did not change.
     */
    void observe(Map verdad);

}
