package com.apple.iad.rhq.mongodb;

import java.net.UnknownHostException;

import org.testng.annotations.Test;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ReplicaSetStatus;

/**
 * Test admin:
 *
 * {
   "serverUsed":"/127.0.0.1:27017",
   "host":"eroero.local",
   "version":"1.8.5",
   "process":"mongod",
   "uptime":263.0,
   "uptimeEstimate":240.0,
   "localTime":{
      "$date":"2013-03-18T20:35:37.006Z"
   },
   "globalLock":{
      "totalTime":2.62192943E8,
      "lockTime":228.0,
      "ratio":8.695886219942998E-7,
      "currentQueue":{
         "total":0,
         "readers":0,
         "writers":0
      },
      "activeClients":{
         "total":0,
         "readers":0,
         "writers":0
      }
   },
   "mem":{
      "bits":64,
      "resident":4,
      "virtual":2448,
      "supported":true,
      "mapped":0
   },
   "connections":{
      "current":1,
      "available":203
   },
   "extra_info":{
      "note":"fields vary by platform"
   },
   "indexCounters":{
      "btree":{
         "accesses":0,
         "hits":0,
         "misses":0,
         "resets":0,
         "missRatio":0.0
      }
   },
   "backgroundFlushing":{
      "flushes":4,
      "total_ms":0,
      "average_ms":0.0,
      "last_ms":0,
      "last_finished":{
         "$date":"2013-03-18T20:35:14.825Z"
      }
   },
   "cursors":{
      "totalOpen":0,
      "clientCursors_size":0,
      "timedOut":0
   },
   "network":{
      "bytesIn":58,
      "bytesOut":87,
      "numRequests":1
   },
   "opcounters":{
      "insert":0,
      "query":1,
      "update":0,
      "delete":0,
      "getmore":0,
      "command":2
   },
   "asserts":{
      "regular":0,
      "warning":0,
      "msg":0,
      "user":0,
      "rollovers":0
   },
   "writeBacksQueued":false,
   "ok":1.0
}
 *
 * @author elias
 */
@Test
public class StatTest {

    public void test() throws UnknownHostException {
        MongoClient client = new MongoClient("localhost", 27017);
        StatClient sclient = new StatClient(client);
        p(sclient);
    }

    private void p(Object o) {
        System.out.println(o);
    }


}
