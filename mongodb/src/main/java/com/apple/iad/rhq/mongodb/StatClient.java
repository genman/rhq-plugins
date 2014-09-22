package com.apple.iad.rhq.mongodb;

import java.util.Map;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

/**
 * Statistics client.
 * Calls 'serverStatus' on the Mongo database and stores the stats for retreival.
 */
public class StatClient {

    private final CommandResult res;

    static final String ADMIN = "admin";

    /**
     * Constructs with DB admin.
     * @throws MongoException if stats cannot be found
     */
    public StatClient(DB admin) {
        res = admin.command("serverStatus");
        res.throwOnError();
    }

    public StatClient(MongoClient client) {
        this(client.getDB("admin"));
    }

    /**
     * Return the double value of a key.
     */
    public double getDouble(String key) {
        return res.getDouble(key);
    }

    /**
     * Return the string value of a key.
     */
    public String getString(String key) {
        return res.getString(key);
    }

    /**
     * Returns a subtree or leaf value.
     * The first item is the first key, etc.
     */
    public Object get(String... key) {
        Map map = res;
        for (String k : key) {
            Object object = map.get(k);
            if (object instanceof Map) {
                map = (Map)object;
            } else {
                return object;
            }
        }
        return map;
    }

    /**
     * Debug string.
     */
    @Override
    public String toString() {
        return "StatClient [res=" + res + "]";
    }

}
