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

import com.google.cloud.storage.BucketInfo;
import org.apache.solr.cloud.api.collections.AbstractBackupRepositoryTest;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.backup.repository.BackupRepository;
import org.junit.AfterClass;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Unit tests for {@link GCSBackupRepository} that use an in-memory Storage object
 */
public class GCSBackupRepositoryTest extends AbstractBackupRepositoryTest {

    @AfterClass
    public static void tearDownClass() throws Exception {
        LocalStorageGCSBackupRepository.clearStashedStorage();
    }

    static boolean alreadyInitd = false;

    @Override
    @SuppressWarnings("rawtypes")
    protected BackupRepository getRepository() {
        final NamedList<Object> config = new NamedList<>();
        config.add(CoreAdminParams.BACKUP_LOCATION, "backup1");
        config.add("bucket", "jegerlowtestbucket2");
        config.add("gcsCredentialPath", "/Users/jasongerlowski/.google_account_key");
        final GCSBackupRepository repository = new GCSBackupRepository();
        repository.init(config);

        try {
            if (!alreadyInitd) {
                //repository.deleteDirectory(new URI("/"));
                repository.storage.create(BucketInfo.newBuilder("jegerlowtestbucket2").build());
                repository.createDirectory(new URI("backup1"));
                System.exit(0);
                alreadyInitd = true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return repository;
    }

    @Override
    protected URI getBaseUri() throws URISyntaxException {
        return new URI("/tmp23");
    }
}
