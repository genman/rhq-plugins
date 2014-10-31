RHQ Agent Plugins
==================

RHQ agent plugins developed by iAd.

These are designed to work with the RHQ project: https://github.com/rhq-project/rhq

Detailed documentation is within the plugin descriptor, but here is a summary of the plugins

# Base plugins

## testing

Not a plugin, but a package used to unit test plugins.

Note that many plugins rely on active services to test, for example 'memcached'
you must test this plugin by actually running 'memcached' on your host. For
these packages, tests are skipped.

## http

Fairly generic URL monitoring plugin. Note that the plugin is called 'http' but
can be used for any URL that Java supports, including file system files.

This is used by many plugins to monitor services that provide JSON through http,
for instance.

## jmx-app

Extends the JMX plugin, for additional plugins to group MBeans under a single
application resource.

## tomcat-http

Helper library that identifies the port number Tomcat is configured under,
allowing http resources to be identified regardless of the port number.

For example, if you want to monitor a resource under the path
`/app/service.json`, you can use `TomcatHttpComponent` as the resource class,
and then indicate the URL as `http://localhost:@PORT@/app/service.json` in your
plugin descriptor.

## port

Simple addressa and port monitoring, although supports discovery of a list of
ports coming from a file or URL source. Captures connection statistics.

## snmp

Generic SNMP monitoring for anything SNMP, up to SNMPv3.

Contains a plugin descriptor generator for any MIB file. This can be invoked,
for example, like so:

c=com.apple.iad.rhq.snmp.PluginGen
mvn exec:java -Dexec.mainClass=$c -Dexec.args="NS-MIB-smiv1.mib" > rhq-plugin.xml

See netscaler plugin for an example.

# Specific plugins

## hadoop

Monitors Apache Hadoop, HDFS, and HBase using JMX. Mostly used for service
status but also gathers stats for seeing disk usage or memory usage.

## dns

Monitors DNS entries and time to resolve DNS lookups. The importance is to
ensure lookup times are fast.

## du

Monitors disk usage using the UNIX du command. This is to track directory
sizes, primarily for alerting.

## flume

Monitors Apache Flume using JMX. For data pipeline performance monitoring, and
generally checks that Flume is working correctly.

## fusionio

Monitors Fusion I/O devices using SNMP and a publicly available MIB database.
For checking if the device has problems (temperature) or may fail soon.

http://www.fusionio.com/

## hive

Apache Hive, a SQL-like query system on top of Hadoop HDFS. Tracks table and file growth etc.

## datatorrent

See: https://www.datatorrent.com/

A service which runs on top of Apache Hadoop software. It monitors activity
using a web services API. The plugin tracks when a DataTorrent application is
running or has failed, or if the application is running slowly.

## hornetq

Monitors JMS for HornetQ (JBoss software) using the JMX interface. Tracks if message queue size, etc.

Useful for monitoring the stand-alone version of HornetQ.

## memcached

Monitoring Memcached software: http://memcached.org/ Useful to monitor memory
usage, eviction rates, etc.

## mongodb

http://www.mongodb.org/

## netapp

SNMP monitoring for NetApp filer software using SNMP.

## netscaler

SNMP monitoring for Cisco Netscaler. Useful for monitoring SSL certificate validity, etc.

## netstat

Captures network statistics from the netstat command. Mostly TCP stats at the
moment.

## oozie

Apache Oozie; a job scheduling tool. Useful to monitor job successes and failures.

## redis

Monitors stats from Redis, similar to Memcached

See: http://redis.io/

## snmptrapd

Extended version of the RHQ trap daemon. Decodes OIDs using a MIB and has some
basic event severity filtering.

## splunk

Monitors Splunk, for forwarders and servers. Gathers generic statistics using the Splunk library
as well as 'tailing' log files.

## tten

Oracle TimesTen monitoring.

Note to run this uses the TimesTen ODBC library and requires configuring
RHQ to access the native libraries. (There are also issues reloading the
agent library.)

## uptime

Monitors system load average and number of users logged in (mostly a toy project)
