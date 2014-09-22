log4j=$HOME/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar
snmp=$HOME/.m2/repository/org/snmp4j/snmp4j/2.1.0/snmp4j-2.1.0.jar

java -cp $log4j:$snmp org.snmp4j.tools.console.SnmpRequest $@
