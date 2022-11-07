/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.handler.admin.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.snapshots.CollectionSnapshotMetaData;
import org.apache.solr.core.snapshots.SolrSnapshotManager;
import org.apache.solr.jersey.JacksonReflectMapWriter;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.jersey.SolrJerseyResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.zookeeper.KeeperException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.solr.client.solrj.impl.BinaryResponseParser.BINARY_CONTENT_TYPE_V2;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_READ_PERM;

@Path("/collections/{collectionName}/snapshots")
public class ListCollectionSnapshotsAPI extends AdminAPIBase {

    @Inject
    public ListCollectionSnapshotsAPI(CoreContainer coreContainer,
                                      SolrQueryRequest solrQueryRequest,
                                      SolrQueryResponse solrQueryResponse) {
        super(coreContainer, solrQueryRequest, solrQueryResponse);
    }

    @GET
    @Produces({"application/json", "application/xml", BINARY_CONTENT_TYPE_V2})
    @PermissionName(COLL_READ_PERM)
    public ListCollectionSnapshotsAPIResponse listSnapshots(
            @Parameter(
                    description = "The name of the collection to list snapshots for.",
                    required = true)
            @PathParam("collectionName") String collectionName) throws InterruptedException, KeeperException {
        final ListCollectionSnapshotsAPIResponse response = instantiateJerseyResponse(ListCollectionSnapshotsAPIResponse.class);
        final CoreContainer coreContainer = fetchAndValidateZooKeeperAwareCoreContainer();
        recordCollectionForLogAndTracing(collectionName, solrQueryRequest);

        String resolvedCollectionName =
                coreContainer
                        .getZkController()
                        .getZkStateReader()
                        .getAliases()
                        .resolveSimpleAlias(collectionName);
        ClusterState clusterState = coreContainer.getZkController().getClusterState();
        if (!clusterState.hasCollection(resolvedCollectionName)) {
            throw new SolrException(
                    SolrException.ErrorCode.BAD_REQUEST,
                    "Collection '" + resolvedCollectionName + "' does not exist, no action taken.");
        }

        response.snapshots = new HashMap<>();
        SolrZkClient client = coreContainer.getZkController().getZkClient();
        Collection<CollectionSnapshotMetaData> m =
                SolrSnapshotManager.listSnapshots(client, collectionName);
        for (CollectionSnapshotMetaData meta : m) {
            final CollectionSnapshot collectionSnapshot = new CollectionSnapshot();
            collectionSnapshot.name = meta.getName();
            collectionSnapshot.status = meta.getStatus().toString(); // TODO Should this be an enum?
            collectionSnapshot.creationDate = meta.getCreationDate().getTime();

            collectionSnapshot.replicas = new HashMap<>();
            for (CollectionSnapshotMetaData.CoreSnapshotMetaData coreMeta : meta.getReplicaSnapshots()) {
                final ReplicaSnapshot replica = new ReplicaSnapshot();
                replica.core = coreMeta.getCoreName();
                replica.indexDirPath = coreMeta.getIndexDirPath();
                replica.generation = coreMeta.getGenerationNumber();
                replica.shardId = coreMeta.getShardId();
                replica.leader = coreMeta.isLeader();
                replica.files = new ArrayList<>();
                replica.files.addAll(coreMeta.getFiles());

                collectionSnapshot.replicas.put(coreMeta.getCoreName(), replica);
            }

            response.snapshots.put(collectionSnapshot.name, collectionSnapshot);
        }

        return response;
    }

    // TODO The following response classes are near duplicates of CollectionSnapshotMetaData. Merging the two classes
    //  is trickier than it'd appear, as CollectionSnapshotMetaData is also used to represent metadata stored in ZK, and
    //  some of the few changes made in the classes here (e.g. creationDate as a long instead of Date) would modify ZK
    //  data parsing in a possibly breaking way.  We should find a way to resolve this after SOLR-16468 has been
    //  completed.
    //  Combining these classes would also greatly simplify the logic in ListCollectionSnapshotsAPI.listSnapshots above.
    public static class ListCollectionSnapshotsAPIResponse extends SolrJerseyResponse {
        @JsonProperty("snapshots")
        public Map<String,CollectionSnapshot> snapshots;
    }

    public static class CollectionSnapshot implements JacksonReflectMapWriter {
        @JsonProperty("name")
        public String name;

        @JsonProperty("status")
        public String status;

        @JsonProperty("creationDate")
        public Long creationDate;

        @JsonProperty("replicas")
        public Map<String, ReplicaSnapshot> replicas;
    }

    public static class ReplicaSnapshot implements JacksonReflectMapWriter {
        @JsonProperty("core")
        public String core;

        @JsonProperty("indexDirPath")
        public String indexDirPath;

        @JsonProperty("generation")
        public Long generation;

        @JsonProperty("shard_id")
        public String shardId;

        @JsonProperty("leader")
        public Boolean leader;

        @JsonProperty("files")
        public List<String> files;
    }
}
