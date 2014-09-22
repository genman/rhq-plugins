package com.apple.iad.rhq.hadoop;

import java.util.Map;

import org.json.simple.parser.JSONParser;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

/**
 * Job tracker information.
 */
public class TaskTrackerInfo extends HadoopMBean {

    protected void digest(Map<String, Double> values) throws Exception {
        EmsAttribute attribute = getEmsBean().getAttribute("TasksInfoJson");
        Object value = attribute.getValue();
        if (value != null) {
            JSONParser parser = new JSONParser();
            Map<String, Number> map = (Map<String, Number>) parser.parse(value.toString());
            for (Map.Entry<String, Number> me : map.entrySet()) {
                values.put(me.getKey(), me.getValue().doubleValue());
            }
        }
    }

}
