/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.gcs;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import com.google.cloud.storage.testing.RemoteStorageHelper;
import com.google.common.collect.Lists;
import org.apache.solr.cloud.api.collections.AbstractIncrementalBackupTest;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO JEGERLOW won't pass unless I can find library to mock out the 'Storage' class used by GCSBackupRepository
public class GCSIncrementalBackupTest extends AbstractIncrementalBackupTest {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String SOLR_XML = "<solr>\n" +
            "\n" +
            "  <str name=\"shareSchema\">${shareSchema:false}</str>\n" +
            "  <str name=\"configSetBaseDir\">${configSetBaseDir:configsets}</str>\n" +
            "  <str name=\"coreRootDirectory\">${coreRootDirectory:.}</str>\n" +
            "\n" +
            "  <shardHandlerFactory name=\"shardHandlerFactory\" class=\"HttpShardHandlerFactory\">\n" +
            "    <str name=\"urlScheme\">${urlScheme:}</str>\n" +
            "    <int name=\"socketTimeout\">${socketTimeout:90000}</int>\n" +
            "    <int name=\"connTimeout\">${connTimeout:15000}</int>\n" +
            "  </shardHandlerFactory>\n" +
            "\n" +
            "  <solrcloud>\n" +
            "    <str name=\"host\">127.0.0.1</str>\n" +
            "    <int name=\"hostPort\">${hostPort:8983}</int>\n" +
            "    <str name=\"hostContext\">${hostContext:solr}</str>\n" +
            "    <int name=\"zkClientTimeout\">${solr.zkclienttimeout:30000}</int>\n" +
            "    <bool name=\"genericCoreNodeNames\">${genericCoreNodeNames:true}</bool>\n" +
            "    <int name=\"leaderVoteWait\">10000</int>\n" +
            "    <int name=\"distribUpdateConnTimeout\">${distribUpdateConnTimeout:45000}</int>\n" +
            "    <int name=\"distribUpdateSoTimeout\">${distribUpdateSoTimeout:340000}</int>\n" +
            "  </solrcloud>\n" +
            "  \n" +
            "  <backup>\n" +
            "    <repository name=\"trackingBackupRepository\" class=\"org.apache.solr.core.TrackingBackupRepository\"> \n" +
            "      <str name=\"delegateRepoName\">localfs</str>\n" +
            "    </repository>\n" +
            "    <repository name=\"localfs\" class=\"org.apache.solr.gcs.LocalStorageGCSBackupRepository\"> \n" +
            "      <str name=\"bucket\">someBucketName</str>\n" +
            "      <str name=\"location\">backup1</str>\n" +
            "    </repository>\n" +
            "  </backup>\n" +
            "  \n" +
            "</solr>\n";

    private static String backupLocation;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Initialize the bucket and location expected by the repository configuration
//        Storage storage = LocalStorageHelper.customOptions(false).getService();
        //storage.create(BucketInfo.of("someBucketName"));
//        storage.create(BlobInfo.newBuilder("someBucketName", "/backup1/").build());

        configureCluster(NUM_SHARDS)// nodes
                .addConfig("conf1", getFile("conf/solrconfig.xml").getParentFile().toPath())
                .withSolrXml(SOLR_XML)
                .configure();
    }

    @Override
    public String getCollectionNamePrefix() {
        return "backuprestore";
    }

    // TODO JEGERLOW: I removed the leading '/' here
    @Override
    public String getBackupLocation() {
        return "backup1";
    }

}
