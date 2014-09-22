package com.apple.iad.rhq.tten;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.database.BasePooledConnectionProvider;

public class TimesTenPooledConnectionProvider extends BasePooledConnectionProvider {

    protected TimesTenPooledConnectionProvider(Configuration config) throws Exception {
        super(config);
    }

}
