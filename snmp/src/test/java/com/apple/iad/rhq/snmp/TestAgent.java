package com.apple.iad.rhq.snmp;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.CoexistenceInfo;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * Simple SNMP agent for testing purpose.
 */
public class TestAgent {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Agent description.
     */
    public static final String DESCRIPTION = "TestAgent 1.0";

    /**
     * Agent 'sysServices' value.
     */
    public static final int SERVICES = 64;

    /**
     * Agent sys OID.
     */
    public static final OID sysOID = new OID("1.3.6.1.4.1.63.1000");

    private final TransportMapping transportMapping;
    private final MessageDispatcherImpl dispatcher;
    private final DefaultMOServer server;
    private Snmp session;
    private OctetString defaultContext = null;

    /**
     * Construct a new instance using a port
     * @param port
     * @throws IOException
     */
    public TestAgent(int port) throws IOException {
        transportMapping = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/" + port));
        this.dispatcher = new MessageDispatcherImpl();
        OctetString engineId = new OctetString(MPv3.createLocalEngineID());
        dispatcher.addMessageProcessingModel(new MPv1());
        dispatcher.addMessageProcessingModel(new MPv2c());

        server = new DefaultMOServer();
        // log.debug("registry " + server.getManagedObject(key, context)getRegistry());

        CommandProcessor processor = new CommandProcessor(engineId) {

            // override since we don't have coexistence info
            @Override
            protected OctetString getViewName(CommandResponderEvent req, CoexistenceInfo cinfo, int viewType) {
                return new OctetString();
            }

            public MOServer getServer(OctetString context) {
                return server;
            }

        };
        dispatcher.addCommandResponder(processor);
        processor.setVacm(new TestVACM());

        Integer32 sysServices = new Integer32(SERVICES);
        OctetString sysDesc = new OctetString(DESCRIPTION);
        SNMPv2MIB snmpv2MIB = new SNMPv2MIB(sysDesc, sysOID, sysServices);
        snmpv2MIB.setName(new OctetString(getClass().getSimpleName()));

        try {
            snmpv2MIB.registerMOs(server, defaultContext);
        } catch (DuplicateRegistrationException e) {
            throw new IllegalStateException(e);
        }

        ManagedObject mo = server.getManagedObject(SnmpConstants.sysDescr, null);
        log.debug("XXX " + mo);
        log.debug("registry " + server.getRegistry());
        log.debug(Arrays.toString(server.getContexts()));
    }

    /**
     * Add an OID with ID, access, and value.
     * @throws DuplicateRegistrationException
     */
    public void addOID(OID id, MOAccess access, Variable value) throws DuplicateRegistrationException {
        server.register(new MOScalar(id, access, value), defaultContext);
    }

    /**
     * Add an OID with ID, RW access, and empty string value.
     * @throws DuplicateRegistrationException
     */
    public void register(OID id) throws DuplicateRegistrationException {
        MOAccess rw = MOAccessImpl.ACCESS_READ_WRITE;
        addOID(id, rw, new OctetString(""));
    }

    public void start() throws IOException {
        this.session = new Snmp(dispatcher, transportMapping);
        this.session.listen();
    }

    public void stop() throws IOException {
        this.session.close();
    }
}
