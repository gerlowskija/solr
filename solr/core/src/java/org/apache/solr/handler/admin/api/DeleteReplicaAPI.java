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

import static org.apache.solr.cloud.Overseer.QUEUE_OPERATION;
import static org.apache.solr.cloud.api.collections.CollectionHandlingUtils.ONLY_IF_DOWN;
import static org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST;
import static org.apache.solr.common.cloud.ZkStateReader.COLLECTION_PROP;
import static org.apache.solr.common.cloud.ZkStateReader.SHARD_ID_PROP;
import static org.apache.solr.common.params.CollectionAdminParams.COUNT_PROP;
import static org.apache.solr.common.params.CollectionAdminParams.FOLLOW_ALIASES;
import static org.apache.solr.common.params.CommonAdminParams.ASYNC;
import static org.apache.solr.common.params.CoreAdminParams.DELETE_DATA_DIR;
import static org.apache.solr.common.params.CoreAdminParams.DELETE_INDEX;
import static org.apache.solr.common.params.CoreAdminParams.DELETE_INSTANCE_DIR;
import static org.apache.solr.common.params.CoreAdminParams.REPLICA;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.solr.api.model.SubResponseAccumulatingJerseyResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.api.V2ApiUtils;
import org.apache.solr.jersey.JacksonReflectMapWriter;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/**
 * V2 APIs for deleting one or more existing replicas from one or more shards.
 *
 * <p>These APIs are analogous to the v1 /admin/collections?action=DELETEREPLICA command.
 */
public class DeleteReplicaAPI extends AdminAPIBase {

  @Inject
  public DeleteReplicaAPI(
      CoreContainer coreContainer,
      SolrQueryRequest solrQueryRequest,
      SolrQueryResponse solrQueryResponse) {
    super(coreContainer, solrQueryRequest, solrQueryResponse);
  }

  @DELETE
  @Path("/collections/{collectionName}/shards/{shardName}/replicas/{replicaName}")
  @PermissionName(COLL_EDIT_PERM)
  public SubResponseAccumulatingJerseyResponse deleteReplicaByName(
      @PathParam("collectionName") String collectionName,
      @PathParam("shardName") String shardName,
      @PathParam("replicaName") String replicaName,
      // Optional params below
      @QueryParam(FOLLOW_ALIASES) Boolean followAliases,
      @QueryParam(DELETE_INSTANCE_DIR) Boolean deleteInstanceDir,
      @QueryParam(DELETE_DATA_DIR) Boolean deleteDataDir,
      @QueryParam(DELETE_INDEX) Boolean deleteIndex,
      @QueryParam(ONLY_IF_DOWN) Boolean onlyIfDown,
      @QueryParam(ASYNC) String asyncId)
      throws Exception {
    final var response = instantiateJerseyResponse(SubResponseAccumulatingJerseyResponse.class);
    ensureRequiredParameterProvided(COLLECTION_PROP, collectionName);
    ensureRequiredParameterProvided(SHARD_ID_PROP, shardName);
    ensureRequiredParameterProvided(REPLICA, replicaName);
    fetchAndValidateZooKeeperAwareCoreContainer();
    recordCollectionForLogAndTracing(collectionName, solrQueryRequest);

    final ZkNodeProps remoteMessage =
        createRemoteMessage(
            collectionName,
            shardName,
            replicaName,
            null,
            followAliases,
            deleteInstanceDir,
            deleteDataDir,
            deleteIndex,
            onlyIfDown,
            asyncId);
    submitRemoteMessageAndHandleResponse(
        response, CollectionParams.CollectionAction.DELETEREPLICA, remoteMessage, asyncId);
    return response;
  }

  @DELETE
  @Path("/collections/{collectionName}/shards/{shardName}/replicas")
  @PermissionName(COLL_EDIT_PERM)
  public SubResponseAccumulatingJerseyResponse deleteReplicasByCount(
      @PathParam("collectionName") String collectionName,
      @PathParam("shardName") String shardName,
      @QueryParam(COUNT_PROP) Integer numToDelete,
      // Optional params below
      @QueryParam(FOLLOW_ALIASES) Boolean followAliases,
      @QueryParam(DELETE_INSTANCE_DIR) Boolean deleteInstanceDir,
      @QueryParam(DELETE_DATA_DIR) Boolean deleteDataDir,
      @QueryParam(DELETE_INDEX) Boolean deleteIndex,
      @QueryParam(ONLY_IF_DOWN) Boolean onlyIfDown,
      @QueryParam(ASYNC) String asyncId)
      throws Exception {
    final var response = instantiateJerseyResponse(SubResponseAccumulatingJerseyResponse.class);
    ensureRequiredParameterProvided(COLLECTION_PROP, collectionName);
    ensureRequiredParameterProvided(SHARD_ID_PROP, shardName);
    ensureRequiredParameterProvided(COUNT_PROP, numToDelete);
    fetchAndValidateZooKeeperAwareCoreContainer();
    recordCollectionForLogAndTracing(collectionName, solrQueryRequest);

    final ZkNodeProps remoteMessage =
        createRemoteMessage(
            collectionName,
            shardName,
            null,
            numToDelete,
            followAliases,
            deleteInstanceDir,
            deleteDataDir,
            deleteIndex,
            onlyIfDown,
            asyncId);
    submitRemoteMessageAndHandleResponse(
        response, CollectionParams.CollectionAction.DELETEREPLICA, remoteMessage, asyncId);
    return response;
  }

  public static class ScaleCollectionRequestBody implements JacksonReflectMapWriter {
    public @JsonProperty(value = COUNT_PROP, required = true) Integer numToDelete;
    public @JsonProperty(FOLLOW_ALIASES) Boolean followAliases;
    public @JsonProperty(DELETE_INSTANCE_DIR) Boolean deleteInstanceDir;
    public @JsonProperty(DELETE_DATA_DIR) Boolean deleteDataDir;
    public @JsonProperty(DELETE_INDEX) Boolean deleteIndex;
    public @JsonProperty(ONLY_IF_DOWN) Boolean onlyIfDown;
    public @JsonProperty(ASYNC) String asyncId;

    public static ScaleCollectionRequestBody fromV1Params(SolrParams v1Params) {
      final var requestBody = new ScaleCollectionRequestBody();
      requestBody.numToDelete = v1Params.getInt(COUNT_PROP);
      requestBody.followAliases = v1Params.getBool(FOLLOW_ALIASES);
      requestBody.deleteInstanceDir = v1Params.getBool(DELETE_INSTANCE_DIR);
      requestBody.deleteDataDir = v1Params.getBool(DELETE_DATA_DIR);
      requestBody.deleteIndex = v1Params.getBool(DELETE_INDEX);
      requestBody.onlyIfDown = v1Params.getBool(ONLY_IF_DOWN);
      requestBody.asyncId = v1Params.get(ASYNC);

      return requestBody;
    }
  }

  @PUT
  @Path("/collections/{collectionName}/scale")
  @PermissionName(COLL_EDIT_PERM)
  public SubResponseAccumulatingJerseyResponse deleteReplicasByCountAllShards(
      @PathParam("collectionName") String collectionName, ScaleCollectionRequestBody requestBody)
      throws Exception {
    final var response = instantiateJerseyResponse(SubResponseAccumulatingJerseyResponse.class);
    if (requestBody == null) {
      throw new SolrException(BAD_REQUEST, "Request body is required but missing");
    }
    ensureRequiredParameterProvided(COLLECTION_PROP, collectionName);
    ensureRequiredParameterProvided(COUNT_PROP, requestBody.numToDelete);
    fetchAndValidateZooKeeperAwareCoreContainer();
    recordCollectionForLogAndTracing(collectionName, solrQueryRequest);

    final ZkNodeProps remoteMessage =
        createRemoteMessage(
            collectionName,
            null,
            null,
            requestBody.numToDelete,
            requestBody.followAliases,
            requestBody.deleteInstanceDir,
            requestBody.deleteDataDir,
            requestBody.deleteIndex,
            requestBody.onlyIfDown,
            requestBody.asyncId);
    submitRemoteMessageAndHandleResponse(
        response,
        CollectionParams.CollectionAction.DELETEREPLICA,
        remoteMessage,
        requestBody.asyncId);
    return response;
  }

  public static ZkNodeProps createRemoteMessage(
      String collectionName,
      String shardName,
      String replicaName,
      Integer numReplicasToDelete,
      Boolean followAliases,
      Boolean deleteInstanceDir,
      Boolean deleteDataDir,
      Boolean deleteIndex,
      Boolean onlyIfDown,
      String asyncId) {
    final Map<String, Object> remoteMessage = new HashMap<>();
    remoteMessage.put(QUEUE_OPERATION, CollectionParams.CollectionAction.DELETEREPLICA.toLower());
    remoteMessage.put(COLLECTION_PROP, collectionName);
    insertIfNotNull(remoteMessage, SHARD_ID_PROP, shardName);
    insertIfNotNull(remoteMessage, REPLICA, replicaName);
    insertIfNotNull(remoteMessage, COUNT_PROP, numReplicasToDelete);
    insertIfNotNull(remoteMessage, FOLLOW_ALIASES, followAliases);
    insertIfNotNull(remoteMessage, DELETE_INSTANCE_DIR, deleteInstanceDir);
    insertIfNotNull(remoteMessage, DELETE_DATA_DIR, deleteDataDir);
    insertIfNotNull(remoteMessage, DELETE_INDEX, deleteIndex);
    insertIfNotNull(remoteMessage, ONLY_IF_DOWN, onlyIfDown);
    insertIfNotNull(remoteMessage, ASYNC, asyncId);

    return new ZkNodeProps(remoteMessage);
  }

  public static void invokeWithV1Params(
      CoreContainer coreContainer,
      SolrQueryRequest solrQueryRequest,
      SolrQueryResponse solrQueryResponse)
      throws Exception {
    final var v1Params = solrQueryRequest.getParams();
    v1Params.required().check(COLLECTION_PROP);

    final var deleteReplicaApi =
        new DeleteReplicaAPI(coreContainer, solrQueryRequest, solrQueryResponse);
    final var v2Response = invokeApiMethod(deleteReplicaApi, v1Params);
    V2ApiUtils.squashIntoSolrResponseWithoutHeader(solrQueryResponse, v2Response);
  }

  private static SubResponseAccumulatingJerseyResponse invokeApiMethod(
      DeleteReplicaAPI deleteReplicaApi, SolrParams v1Params) throws Exception {
    if (v1Params.get(REPLICA) != null && v1Params.get(SHARD_ID_PROP) != null) {
      return deleteReplicaApi.deleteReplicaByName(
          v1Params.get(COLLECTION_PROP),
          v1Params.get(SHARD_ID_PROP),
          v1Params.get(REPLICA),
          v1Params.getBool(FOLLOW_ALIASES),
          v1Params.getBool(DELETE_INSTANCE_DIR),
          v1Params.getBool(DELETE_DATA_DIR),
          v1Params.getBool(DELETE_INDEX),
          v1Params.getBool(ONLY_IF_DOWN),
          v1Params.get(ASYNC));
    } else if (v1Params.get(SHARD_ID_PROP) != null
        && v1Params.get(COUNT_PROP) != null) { // Delete 'N' replicas
      return deleteReplicaApi.deleteReplicasByCount(
          v1Params.get(COLLECTION_PROP),
          v1Params.get(SHARD_ID_PROP),
          v1Params.getInt(COUNT_PROP),
          v1Params.getBool(FOLLOW_ALIASES),
          v1Params.getBool(DELETE_INSTANCE_DIR),
          v1Params.getBool(DELETE_DATA_DIR),
          v1Params.getBool(DELETE_INDEX),
          v1Params.getBool(ONLY_IF_DOWN),
          v1Params.get(ASYNC));
    } else if (v1Params.get(COUNT_PROP) != null) {
      return deleteReplicaApi.deleteReplicasByCountAllShards(
          v1Params.get(COLLECTION_PROP), ScaleCollectionRequestBody.fromV1Params(v1Params));
    } else {
      throw new SolrException(
          BAD_REQUEST,
          "DELETEREPLICA requires either " + COUNT_PROP + " or " + REPLICA + " parameters");
    }
  }
}
