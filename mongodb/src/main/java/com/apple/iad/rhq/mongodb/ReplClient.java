package com.apple.iad.rhq.mongodb;

import java.util.List;
import java.util.Map;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

/**
 * Replication state client.
 */
public class ReplClient {

    private final CommandResult repl;

    /**
     * Constructs a new instance.
     * @throws MongoException if replication not on, or other reasons
     */
    public ReplClient(DB admin) {
        repl = admin.command("replSetGetStatus");
        repl.throwOnError();
    }

    public ReplClient(MongoClient client) {
        this(client.getDB(StatClient.ADMIN));
    }

    /**
     * Returns this self member.
     */
    public Member getSelf() {
        List<Map> l = (List<Map>) repl.get("members");
        for (Map<String, ?> m : l) {
            if (Boolean.TRUE.equals(m.get("self"))) {
                return new Member(m);
            }
        }
        return null;
    }

    /**
     * Returns the primary state member; null if not known.
     */
    public Member getPrimary() {
        List<Map> l = (List<Map>) repl.get("members");
        for (Map<String, ?> m : l) {
            if (Integer.valueOf(1).equals(m.get("state"))) {
                return new Member(m);
            }
        }
        return null;
    }

    /**
     * Returns the replication set, or null if not known.
     */
    public String getSet() {
        return (String) repl.get("set");
    }

    /**
     * Returns the replication state; never null.
     */
    public ReplState getState() {
        Number n = (Number) repl.get("myState");
        if (n != null)
            return ReplState.values()[n.intValue()];
        return ReplState.UNKNOWN;
    }

    /**
     * Replication member.
     */
    public static class Member {

        private final Map<String, ?> m;

        /**
         * Fields we care about.
         */
        enum Field {
            /**
             * Server name.
             */
            name,
            /**
             * Health state.
             */
            health,
            /**
             * State value.
             */
            stateStr,
            uptime,
            self,
        }

        Member(Map<String, ?> m) {
            this.m = m;
        }

        Object get(Field field) {
            return m.get(field.name());
        }

        @Override
        public String toString() {
            return "Member [m=" + m + "]";
        }

    }

    @Override
    public String toString() {
        return "StatClient [repl=" + repl + "]";
    }

    /**
     * Returns true if this is a master or not by querying the server.
     */
    public static boolean isMaster(MongoClient client) {
        DB admin = client.getDB(StatClient.ADMIN);
        CommandResult master = admin.command("isMaster");
        master.throwOnError();
        return Boolean.TRUE.equals(master.get("ismaster"));
    }

}
