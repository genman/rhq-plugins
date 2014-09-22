package com.apple.iad.rhq.redis;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Adds the INFO all commands.
 */
public class Client2 extends Client {

    public Client2(String host, int port) {
        super(host, port);
    }

    /**
     * Sends the "INFO all" command.
     */
    public Properties infoAll() {
        sendCommand(Command.INFO, "all");
        String reply = getBulkReply();
        Properties p = new Properties();
        try {
            p.load(new StringReader(reply));
        } catch (IOException e) {
            throw new JedisDataException(e);
        }
        return p;
    }
}
