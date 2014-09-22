#!/bin/sh
c=com.apple.iad.rhq.snmp.PluginGen
mvn exec:java -Dexec.mainClass=$c -Dexec.args="NS-MIB-smiv1.mib" > rhq-plugin.xml
