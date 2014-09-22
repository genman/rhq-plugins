package com.apple.iad.rhq.mongodb;

import java.net.UnknownHostException;

import org.testng.annotations.Test;

import com.apple.iad.rhq.mongodb.ReplClient.Member;
import com.apple.iad.rhq.mongodb.ReplClient.Member.Field;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

/**
 * Test replication state:
 *

date":"2013-03-20T20:02:03.000Z"
   },
   "myState":1,
   "members":[
      {
         "_id":0,
         "name":"hostname:27017",
         "health":1.0,
         "state":2,
         "stateStr":"SECONDARY",
         "uptime":2220,
         "optime":{
            "$ts":1363741915,
            "$inc":7
         },
         "optimeDate":{
            "$date":"2013-03-20T01:11:55.000Z"
         },
         "lastHeartbeat":{
            "$date":"2013-03-20T20:02:02.000Z"
         },
         "pingMs":0
      },
      {
         "_id":1,
         "name":"hostname:27018",
         "health":1.0,
         "state":1,
         "stateStr":"PRIMARY",
         "optime":{
            "$ts":1363741915,
            "$inc":7
         },
         "optimeDate":{
            "$date":"2013-03-20T01:11:55.000Z"
         },
         "self":true
      },
      {
         "_id":2,
         "name":"hostname:27019",
         "health":1.0,
         "state":2,
         "stateStr":"SECONDARY",
         "uptime":2210,
         "optime":{
            "$ts":1363741915,
            "$inc":7
         },
         "optimeDate":{
            "$date":"2013-03-20T01:11:55.000Z"
         },
         "lastHeartbeat":{
            "$date":"2013-03-20T20:02:02.000Z"
         },
         "pingMs":0
      }
   ],
   "ok":1.0
}

 *
 * @author elias
 */
@Test
public class ReplTest {

    public void test() throws UnknownHostException {

        MongoClient client = new MongoClient("localhost", 27017);
        assert ReplClient.isMaster(client);

        try {
            ReplClient repl = new ReplClient(client);

            assert repl.getSelf() != null;
            assert repl.getSet() != null;
            assert repl.getState() != null;
            Member primary = repl.getPrimary();
            assert primary != null;
            assert primary.get(Field.stateStr) != null;
            assert primary.get(Field.name) != null;
        } catch (MongoException e) {
            System.out.println(e);
            System.out.println(e.getClass());
        }

    }

}
