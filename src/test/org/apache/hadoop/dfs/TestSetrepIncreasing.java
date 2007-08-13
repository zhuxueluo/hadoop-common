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
package org.apache.hadoop.dfs;

import junit.framework.TestCase;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

public class TestSetrepIncreasing extends TestCase {
  static void setrep(int fromREP, int toREP) throws IOException {
    Configuration conf = new Configuration();
    conf.set("dfs.replication", "" + fromREP);
    conf.setLong("dfs.blockreport.intervalMsec", 1000L);
    MiniDFSCluster cluster = new MiniDFSCluster(conf, 10, true, null);
    FileSystem fs = cluster.getFileSystem();
    assertTrue("Not a HDFS: "+fs.getUri(), fs instanceof DistributedFileSystem);

    try {
      Path root = TestDFSShell.mkdir(fs, 
          new Path("/test/setrep" + fromREP + "-" + toREP));
      Path f = TestDFSShell.writeFile(fs, new Path(root, "foo"));
      
      // Verify setrep for changing replication
      {
        String[] args = {"-setrep", "-w", "" + toREP, "" + f};
        FsShell shell = new FsShell();
        shell.setConf(conf);
        try {
          assertEquals(0, shell.run(args));
        } catch (Exception e) {
          assertTrue("-setrep " + e, false);
        }
      }

      //get fs again since the old one may be closed
      fs = cluster.getFileSystem();
      long len = fs.getFileStatus(f).getLen();
      for(String[] locations : fs.getFileCacheHints(f, 0, len)) {
        assertTrue(locations.length == toREP);
      }
      TestDFSShell.show("done setrep waiting: " + root);
    } finally {
      try {fs.close();} catch (Exception e) {}
      cluster.shutdown();
    }
  }

  public void testSetrepIncreasing() throws IOException {
    setrep(3, 7);
  }
}