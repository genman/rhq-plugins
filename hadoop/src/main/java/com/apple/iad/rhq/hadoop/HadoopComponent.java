
package com.apple.iad.rhq.hadoop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * Basic component.
 */
public class HadoopComponent extends JMXServerComponent<HadoopMBean>
{
    protected Log log = LogFactory.getLog(getClass());
}
