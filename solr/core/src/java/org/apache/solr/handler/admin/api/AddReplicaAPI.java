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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.api.Command;
import org.apache.solr.api.EndPoint;
import org.apache.solr.api.PayloadObj;
import org.apache.solr.client.solrj.request.beans.AddReplicaPayload;
import org.apache.solr.client.solrj.request.beans.SplitShardPayload;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CommonAdminParams;
import org.apache.solr.handler.admin.CollectionsHandler;

import java.util.HashMap;
import java.util.Map;

import static org.apache.solr.client.solrj.SolrRequest.METHOD.POST;
import static org.apache.solr.common.params.CollectionAdminParams.*;
import static org.apache.solr.common.params.CommonParams.ACTION;
import static org.apache.solr.handler.ClusterAPI.wrapParams;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;

/**
 * V2 API for adding a new replica to an existing shard.
 *
 * This API (POST /v2/collections/collectionName/shards {'add-replica': {...}}) is analogous to the v1
 * /admin/collections?action=ADDREPLICA command.
 *
 * @see AddReplicaPayload
 */
@EndPoint(
        path = {"/c/{collection}/shards", "/collections/{collection}/shards"},
        method = POST,
        permission = COLL_EDIT_PERM)
public class AddReplicaAPI {
  private static final String V2_ADD_REPLICA_CMD = "add-replica";

  private final CollectionsHandler collectionsHandler;

  public AddReplicaAPI(CollectionsHandler collectionsHandler) {
    this.collectionsHandler = collectionsHandler;
  }

  @Command(name = V2_ADD_REPLICA_CMD)
  public void splitShard(PayloadObj<AddReplicaPayload> obj) throws Exception {
    final AddReplicaPayload v2Body = obj.get();
    final Map<String, Object> v1Params = v2Body.toMap(new HashMap<>());
    v1Params.put(ACTION, CollectionParams.CollectionAction.ADDREPLICA.toLower());
    v1Params.put(COLLECTION, obj.getRequest().getPathTemplateValues().get(COLLECTION));

    if (MapUtils.isNotEmpty(v2Body.coreProperties)) {
      flattenMapWithPrefix(v2Body.coreProperties, v1Params, PROPERTY_PREFIX);
    }
    if (CollectionUtils.isNotEmpty(v2Body.createNodeSet)) {
      v1Params.replace(CREATE_NODE_SET_PARAM, String.join(",", v2Body.createNodeSet));
    }
    collectionsHandler.handleRequestBody(wrapParams(obj.getRequest(), v1Params), obj.getResponse());
  }

  private void flattenMapWithPrefix(Map<String, Object> toFlatten, Map<String, Object> destination,
                                    String additionalPrefix) {
    if (toFlatten == null || toFlatten.isEmpty() || destination == null) {
      return;
    }

    toFlatten.forEach((k, v) -> destination.put(additionalPrefix + k, v));
  }
}
