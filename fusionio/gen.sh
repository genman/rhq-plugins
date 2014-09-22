#!/bin/sh
c=com.apple.iad.rhq.snmp.PluginGen
mvn exec:java -Dexec.mainClass=$c -Dexec.args="fioIoDimm.mib" > rhq-plugin.xml
