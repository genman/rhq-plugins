package com.apple.iad.rhq.testing;

import java.util.Map;
import java.util.Map.Entry;

import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.local.LocalVMFinder;
import org.mc4j.ems.connection.local.LocalVirtualMachine;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

public class JmxTree {
    
    static String tab = "    ";

    public JmxTree() {
        Map<Integer, LocalVirtualMachine> mvm = LocalVMFinder.getManageableVirtualMachines();
        for (Entry<Integer, LocalVirtualMachine> lvm : mvm.entrySet()) {
            println(lvm.getKey() + " " + lvm.getValue());
            ConnectionSettings settings = new ConnectionSettings();
            settings.initializeConnectionType(new LocalVMTypeDescriptor());
            settings.setServerUrl(lvm.getKey().toString());
            
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.discoverServerClasses(settings);
            ConnectionProvider provider = connectionFactory.getConnectionProvider(settings);
            EmsConnection connect = provider.connect();
            for (EmsBean bean : connect.getBeans()) {
                println(bean.getBeanName().toString());
                for (EmsAttribute ea : bean.getAttributes()) {
                    try {
                        println(tab + ea.getName() + " type=" + ea.getType() + " value=" + ea.getValue());
                    } catch (Exception e) {
                        println(tab + e);
                    }
                }
                for (EmsOperation eo : bean.getOperations()) {
                    try {
                        println(tab + eo.getName() + " type=" + eo.getReturnType());
                    } catch (Exception e) {
                        println(tab + e);
                    }
                }
            }
            println("");
        }
        
    }

    private void println(String string) {
        System.out.println(string);
    }
    
    public static void main(String s[]) {
        new JmxTree();
    }
}
