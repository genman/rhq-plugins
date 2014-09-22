#!/bin/sh
c=apple.iad.rhq.snmp.PluginGen
mvn exec:java -Dexec.mainClass=$c -Dexec.args="NETWORK-APPLIANCE-MIB.mib" > rhq-plugin.xml
