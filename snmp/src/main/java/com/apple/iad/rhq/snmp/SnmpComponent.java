package com.apple.iad.rhq.snmp;

import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.CounterSupport;
import org.snmp4j.mp.DefaultCounterListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.TSM;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TlsAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.TLSTM;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

/**
 * RHQ component that maintains a SNMP connection or transport for performing GET, SET, etc.
 */
public class SnmpComponent implements SnmpComponentHolder, PDUFactory, MeasurementFacet, OperationFacet {

    /**
     * Log handle.
     */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Plugin configuration.
     */
    private Configuration conf;

    /**
     * Main SNMP instance we manage.
     */
    private Snmp snmp;

    // SNMP transport options

    private Target target;
    private Address address;
    private OID authProtocol;
    private OID privProtocol;
    private OctetString privPassphrase;
    private OctetString authPassphrase;
    private OctetString authoritativeEngineID;
    private OctetString securityName = new OctetString();

    private OctetString contextEngineID = null;
    private OctetString contextName = null;
    private final OctetString localEngineID = new OctetString(MPv3.createLocalEngineID());

    private int version = 3;
    private int engineBootCount = 0;
    private int retries = 1;
    private int timeout = 1000;
    private int maxSizeResponsePDU = 65535;

    static {
        CounterSupport.getInstance().addCounterListener(new DefaultCounterListener());
    }

    /**
     * Constructs a dummy instance.
     */
    public SnmpComponent() {
    }

    /**
     * Constructor used during discovery to obtain version and description.
     * Call {@link #stop()} once done.
     */
    SnmpComponent(Configuration conf) throws IOException {
        this.conf = conf;
        init();
    }

    public Map<OID, Variable> get(Collection<OID> oids) throws IOException {
        Map<OID, Variable> map = new LinkedHashMap<OID, Variable>();
        PDU request = createPDU();
        request.setType(PDU.GET);
        request.setVariableBindings(wrap(oids));
        boolean debug = log.isDebugEnabled();
        if (debug)
            log.debug("request " + request);
        ResponseEvent re = snmp.send(request, this.target);
        PDU response = re.getResponse();
        if (debug)
            log.debug("response " + response);
        if (response == null) {
            throw new NoResponseException("no response querying " + oids);
        }
        if (re.getError() != null)
            throw new IOException(re.getError());
        Vector<? extends VariableBinding> vbs = response.getVariableBindings();
        if (vbs.size() == 1) {
            String error = Errors.get(vbs.get(0).getOid());
            if (error != null)
                throw new IOException("got SNMP error " + error);
        }
        for (VariableBinding vb : vbs) {
            OID oid = vb.getOid();
            map.put(oid, vb.getVariable());
        }
        return map;
    }

    public Variable get(OID oid) throws IOException {
        Map<OID, Variable> map = get(Collections.singleton(oid));
        if (map.isEmpty())
            throw new NoResponseException("no response for " + oid);
        return map.values().iterator().next();
    }

    private List<? extends VariableBinding> wrap(Collection<OID> oids) {
        List<VariableBinding> l = new ArrayList<VariableBinding>();
        for (OID oid : oids)
            l.add(new VariableBinding(oid));
        return l;
    }

    @Override
    public AvailabilityType getAvailability() {
        try {
            OID oid = SnmpConstants.sysUpTime;
            Map<OID, Variable> map = get(Collections.singletonList(oid));
            Variable variable = map.get(oid);
            if (variable == null)
                throw new Exception("not found " + map);
            log.debug("uptime " + variable);
            return AvailabilityType.UP;
        } catch (Exception e) {
            log.info("SNMP unavailable", e);
            return AvailabilityType.DOWN;
        }
    }

    /**
     * Creates a PDU using the given SNMP target information.
     */
    @Override
    public PDU createPDU(Target target) {
        PDU request;
        if (target.getVersion() == 3) {
            request = new ScopedPDU();
            ScopedPDU scopedPDU = (ScopedPDU)request;
            if (this.contextEngineID != null)
                scopedPDU.setContextEngineID(this.contextEngineID);
            if (this.contextName != null)
                scopedPDU.setContextName(this.contextName);
        } else {
            request = new PDU();
        }
        return request;
    }

    private PDU createPDU() {
        return createPDU(target);
    }

    private String conf(String key) {
        return conf(key, "");
    }

    private String conf(String key, Object def) {
        return conf.getSimpleValue(key, def.toString());
    }

    private int conf(String key, int value) {
        return parseInt(conf.getSimpleValue(key, Integer.toString(value)));
    }

    private int getVersion() {
        return conf("version", 3);
    }

    private static OctetString createOctetString(String s) {
        if (s.startsWith("0x")) {
            return OctetString.fromHexString(s.substring(2), ':');
        } else {
            return new OctetString(s);
        }
    }

    @Override
    public void start(ResourceContext rc) throws InvalidPluginConfigurationException, Exception
    {
        conf = rc.getPluginConfiguration();
        init();
    }

    private void init() throws IOException {
        String s;
        // transport
        this.address = getAddress();
        this.version = getVersion();
        this.retries = conf("retries", retries);
        this.timeout = conf("timeout", timeout);
        this.maxSizeResponsePDU = conf("maxSizeResponsePDU", maxSizeResponsePDU);
        this.engineBootCount = conf("engineBootCount", engineBootCount);
        s = conf("contextEngineID");
        if (set(s))
            this.contextEngineID = createOctetString(s);
        s = conf("contextName");
        if (set(s))
            this.contextName = createOctetString(s);

        // privacy
        this.privProtocol = privProtocol();
        this.privPassphrase = createOctetString(conf("privPassphrase"));

        // authorization
        this.authProtocol = authProtocol();
        this.authPassphrase = createOctetString(conf("authPassphrase"));
        this.securityName = createOctetString(conf("securityName", "public"));

        s = conf("authoritativeEngineID");
        if (set(s))
            this.authoritativeEngineID = createOctetString(s);

        this.snmp = createSnmpSession();
        this.target = createTarget();
        this.target.setVersion(this.version);
        this.target.setAddress(this.address);
        this.target.setRetries(this.retries);
        this.target.setTimeout(this.timeout);
        this.target.setMaxSizeRequestPDU(this.maxSizeResponsePDU);
        snmp.listen();
    }

    private boolean set(String s) {
        return s.trim().isEmpty();
    }

    enum Transport {
        UDP, TCP, TLS
    }

    enum PrivProtocol {
        NONE, DES, AES128, AES192, AES256, _3DES,
    }

    enum AuthProtocol {
        NONE, MD5, SHA
    }

    private OID authProtocol() {
        AuthProtocol ap = AuthProtocol.valueOf(conf("authProtocol", AuthProtocol.NONE));
        switch (ap) {
        case MD5: return AuthMD5.ID;
        case SHA: return AuthSHA.ID;
        default: return null;
        }
    }

    private OID privProtocol() {
        PrivProtocol pp = PrivProtocol.valueOf(conf("privProtocol", PrivProtocol.NONE));
        switch (pp) {
        case DES: return PrivDES.ID;
        case AES128: return PrivAES128.ID;
        case AES192: return PrivAES192.ID;
        case AES256: return PrivAES256.ID;
        case _3DES: return Priv3DES.ID;
        default: return null;
        }
    }

    private Address getAddress() {
        Transport t = Transport.valueOf(conf("transport", Transport.UDP));
        String transportAddress = conf("transportAddress");
        if (transportAddress.indexOf('/') == -1) {
            transportAddress = transportAddress + "/161";
        }
        switch (t) {
        case UDP: return new UdpAddress(transportAddress);
        case TCP: return new TcpAddress(transportAddress);
        case TLS: return new TlsAddress(transportAddress);
        }
        throw new IllegalArgumentException("Unknown transport");
    }

    private Target createTarget() {
        if (this.version == 3) {
            UserTarget target = new UserTarget();
            if (this.authPassphrase != null) {
                if (this.privPassphrase != null) {
                    target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                } else {
                    target.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
                }
            } else {
                target.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
            }
            target.setSecurityName(this.securityName);
            if (this.authoritativeEngineID != null) {
                target.setAuthoritativeEngineID(this.authoritativeEngineID.getValue());
            }
            if (this.address instanceof TlsAddress) {
                target.setSecurityModel(SecurityModel.SECURITY_MODEL_TSM);
            }
            return target;
        }

        // SNMPv2 and lower use the security name as the community string

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(this.securityName);
        return target;
    }

    private Snmp createSnmpSession() throws IOException {
        AbstractTransportMapping transport;
        if (this.address instanceof TlsAddress) {
            transport = new TLSTM();
        } else {
            if (this.address instanceof TcpAddress) {
                transport = new DefaultTcpTransportMapping();
            } else {
                transport = new DefaultUdpTransportMapping();
            }
        }

        Snmp snmp = new Snmp(transport);
        int v = MessageProcessingModel.MPv3;
        ((MPv3) snmp.getMessageProcessingModel(v)).setLocalEngineID(this.localEngineID.getValue());

        if (this.version == 3) {
            USM usm = new USM(SecurityProtocols.getInstance(), this.localEngineID, this.engineBootCount);
            SecurityModels.getInstance().addSecurityModel(usm);
            addUsmUser(snmp);
            SecurityModels.getInstance().addSecurityModel(new TSM(this.localEngineID, false));
        }

        return snmp;
    }

    private void addUsmUser(Snmp snmp) {
        snmp.getUSM().addUser(this.securityName, new UsmUser(this.securityName,
                this.authProtocol, this.authPassphrase,
                this.privProtocol, this.privPassphrase));
    }

    /**
     * Returns a tool to obtain data from tables.
     */
    private TableUtils getTableUtils() {
        return new TableUtils(snmp, this);
    }

    public List<TableEvent> getTable(OID oids[]) {
        TableUtils tableUtils = getTableUtils();
        return tableUtils.getTable(target, oids, null, null);
    }

    @Override
    public void stop() {
        try {
            snmp.close();
        } catch (IOException e) {
            log.warn("close", e);
        }
    }

    @Override
    public void getValues(MeasurementReport mr, Set<MeasurementScheduleRequest> requests) throws Exception {
        MibComponent mc = new MibComponent(this);
        mc.getValues(mr, requests);
    }

    @Override
    public String toString() {
        return "SnmpComponent [address=" + address + ", authProtocol="
                + authProtocol + ", privProtocol=" + privProtocol
                + ", version=" + version + "]";
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception {
        if (name.equals("set")) {
            return new SetOperation(this).set(parameters);
        }
        throw new Exception("unknown operation " + name);
    }

    /**
     * Performs an SNMP set operation.
     * @throws IOException
     */
    public ResponseEvent set(VariableBinding vb) throws IOException {
        PDU request = createPDU();
        request.add(vb);
        return snmp.set(request, target);
    }

    @Override
    public SnmpComponent getSnmpComponent() {
        return this;
    }

}