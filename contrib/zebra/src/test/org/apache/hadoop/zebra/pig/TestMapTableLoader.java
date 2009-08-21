/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.zebra.pig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.zebra.io.BasicTable;
import org.apache.hadoop.zebra.io.TableInserter;
import org.apache.hadoop.zebra.io.TableScanner;
import org.apache.hadoop.zebra.io.BasicTable.Reader.RangeSplit;
import org.apache.hadoop.zebra.types.ParseException;
import org.apache.hadoop.zebra.types.Schema;
import org.apache.hadoop.zebra.types.TypesUtils;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.Tuple;
import org.apache.pig.test.MiniCluster;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Note:
 * 
 * Make sure you add the build/pig-0.1.0-dev-core.jar to the Classpath of the
 * app/debug configuration, when run this from inside the Eclipse.
 * 
 */
public class TestMapTableLoader {
  protected static ExecType execType = ExecType.MAPREDUCE;
  private static MiniCluster cluster;
  protected static PigServer pigServer;
  private static Path pathTable;
  private static Configuration conf;

  @BeforeClass
  public static void setUp() throws Exception {
    if (System.getProperty("hadoop.log.dir") == null) {
      String base = new File(".").getPath(); // getAbsolutePath();
      System
          .setProperty("hadoop.log.dir", new Path(base).toString() + "./logs");
    }

    if (execType == ExecType.MAPREDUCE) {
      cluster = MiniCluster.buildCluster();
      pigServer = new PigServer(ExecType.MAPREDUCE, cluster.getProperties());
    } else {
      pigServer = new PigServer(ExecType.LOCAL);
    }

    conf = new Configuration();
    FileSystem fs = cluster.getFileSystem();
    Path pathWorking = fs.getWorkingDirectory();
    pathTable = new Path(pathWorking, "TestMapTableLoader");

    BasicTable.Writer writer = new BasicTable.Writer(pathTable,
        "m1:map(string)", "[m1#{a}]", false, conf);
    Schema schema = writer.getSchema();
    Tuple tuple = TypesUtils.createTuple(schema);

    final int numsBatch = 10;
    final int numsInserters = 2;
    TableInserter[] inserters = new TableInserter[numsInserters];
    for (int i = 0; i < numsInserters; i++) {
      inserters[i] = writer.getInserter("ins" + i, false);
    }

    for (int b = 0; b < numsBatch; b++) {
      for (int i = 0; i < numsInserters; i++) {
        TypesUtils.resetTuple(tuple);
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "x");
        map.put("b", "y");
        map.put("c", "z");
        tuple.set(0, map);

        try {
          inserters[i].insert(new BytesWritable(("key" + i).getBytes()), tuple);
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    }
    for (int i = 0; i < numsInserters; i++) {
      inserters[i].close();
    }
  }

  @AfterClass
  public static void tearDown() throws Exception {
    pigServer.shutdown();
  }

  @Test
  public void test1() throws IOException, ParseException {
    String projection = new String("m1#{b}");
    BasicTable.Reader reader = new BasicTable.Reader(pathTable, conf);
    reader.setProjection(projection);
    // long totalBytes = reader.getStatus().getSize();

    List<RangeSplit> splits = reader.rangeSplit(1);
    reader.close();
    reader = new BasicTable.Reader(pathTable, conf);
    reader.setProjection(projection);

    TableScanner scanner = reader.getScanner(splits.get(0), true);
    BytesWritable key = new BytesWritable();
    Tuple value = TypesUtils.createTuple(scanner.getSchema());
    // HashMap<String, Object> mapval;
    while (!scanner.atEnd()) {
      scanner.getKey(key);
      // Assert.assertEquals(key, new BytesWritable("key0".getBytes()));
      scanner.getValue(value);
      System.out.println("key = " + key + " value = " + value);

      // mapval = (HashMap<String, Object>) value.get(0);
      // Assert.assertEquals("x", mapval.get("a"));
      // Assert.assertEquals(null, mapval.get("b"));
      // Assert.assertEquals(null, mapval.get("c"));
      scanner.advance();
    }
    reader.close();
  }

  @Test
  public void testReader() throws ExecException, IOException {
    String query = "records = LOAD '" + pathTable.toString()
        + "' USING org.apache.hadoop.zebra.pig.TableLoader('m1#{a}');";
    System.out.println(query);
    pigServer.registerQuery(query);
    Iterator<Tuple> it = pigServer.openIterator("records");
    while (it.hasNext()) {
      Tuple cur = it.next();
      System.out.println(cur);
    }
  }
}