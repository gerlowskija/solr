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
package org.apache.solr.handler.admin.api;

import static org.apache.solr.client.solrj.impl.BinaryResponseParser.BINARY_CONTENT_TYPE_V2;
import static org.apache.solr.cloud.Overseer.QUEUE_OPERATION;
import static org.apache.solr.common.cloud.ZkStateReader.COLLECTION_PROP;
import static org.apache.solr.common.params.CollectionAdminParams.FOLLOW_ALIASES;
import static org.apache.solr.common.params.CommonAdminParams.ASYNC;
import static org.apache.solr.handler.admin.CollectionsHandler.DEFAULT_COLLECTION_OP_TIMEOUT;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.admin.CollectionsHandler;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.api.model.AsyncJerseyResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/** V2 API for Deleting Collection Snapshots. */
@Path("/collections/{collName}/snapshots")
public class DeleteCollectionSnapshotAPI extends AdminAPIBase {

  @Inject
  public DeleteCollectionSnapshotAPI(
      CoreContainer coreContainer,
      SolrQueryRequest solrQueryRequest,
      SolrQueryResponse solrQueryResponse) {
    super(coreContainer, solrQueryRequest, solrQueryResponse);
  }

  /** This API is analogous to V1's (POST /solr/admin/collections?action=DELETESNAPSHOT) */
  @DELETE
  @Path("/{snapshotName}")
  @Produces({"application/json", "application/xml", BINARY_CONTENT_TYPE_V2})
  @PermissionName(COLL_EDIT_PERM)
  public DeleteSnapshotResponse deleteSnapshot(
      @Parameter(description = "The name of the collection.", required = true)
          @PathParam("collName")
          String collName,
      @Parameter(description = "The name of the snapshot to be deleted.", required = true)
          @PathParam("snapshotName")
          String snapshotName,
      @Parameter(description = "A flag that treats the collName parameter as a collection alias.")
          @DefaultValue("false")
          @QueryParam("followAliases")
          boolean followAliases,
      @QueryParam("async") String asyncId)
      throws Exception {
    final DeleteSnapshotResponse response = instantiateJerseyResponse(DeleteSnapshotResponse.class);
    final CoreContainer coreContainer = fetchAndValidateZooKeeperAwareCoreContainer();
    recordCollectionForLogAndTracing(collName, solrQueryRequest);

    final String collectionName = resolveCollectionName(collName, followAliases);

    final ZkNodeProps remoteMessage =
        createRemoteMessage(collectionName, followAliases, snapshotName, asyncId);
    final SolrResponse remoteResponse =
        CollectionsHandler.submitCollectionApiCommand(
            coreContainer,
            coreContainer.getDistributedCollectionCommandRunner(),
            remoteMessage,
            CollectionParams.CollectionAction.DELETESNAPSHOT,
            DEFAULT_COLLECTION_OP_TIMEOUT);

    if (remoteResponse.getException() != null) {
      throw remoteResponse.getException();
    }

    response.collection = collName;
    response.snapshotName = snapshotName;
    response.followAliases = followAliases;
    response.requestId = asyncId;

    return response;
  }

  /**
   * The Response for {@link DeleteCollectionSnapshotAPI}'s {@link #deleteSnapshot(String, String,
   * boolean, String)}
   */
  public static class DeleteSnapshotResponse extends AsyncJerseyResponse {
    @Schema(description = "The name of the collection.")
    @JsonProperty(COLLECTION_PROP)
    String collection;

    @Schema(description = "The name of the snapshot to be deleted.")
    @JsonProperty("snapshot")
    String snapshotName;

    @Schema(description = "A flag that treats the collName parameter as a collection alias.")
    @JsonProperty("followAliases")
    boolean followAliases;
  }

  public static ZkNodeProps createRemoteMessage(
      String collectionName, boolean followAliases, String snapshotName, String asyncId) {
    final Map<String, Object> remoteMessage = new HashMap<>();

    remoteMessage.put(QUEUE_OPERATION, CollectionParams.CollectionAction.DELETESNAPSHOT.toLower());
    remoteMessage.put(COLLECTION_PROP, collectionName);
    remoteMessage.put(CoreAdminParams.COMMIT_NAME, snapshotName);
    remoteMessage.put(FOLLOW_ALIASES, followAliases);

    if (asyncId != null) remoteMessage.put(ASYNC, asyncId);

    return new ZkNodeProps(remoteMessage);
  }
}
