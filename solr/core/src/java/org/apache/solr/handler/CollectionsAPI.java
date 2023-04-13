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

package org.apache.solr.handler;

import org.apache.solr.api.Command;
import org.apache.solr.api.EndPoint;
import org.apache.solr.api.PayloadObj;
import org.apache.solr.client.solrj.request.beans.RestoreCollectionPayload;
import org.apache.solr.client.solrj.request.beans.V2ApiConstants;
import org.apache.solr.common.params.CollectionAdminParams;
import org.apache.solr.common.params.CollectionParams.CollectionAction;
import org.apache.solr.handler.admin.CollectionsHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.solr.client.solrj.SolrRequest.METHOD.POST;
import static org.apache.solr.client.solrj.request.beans.V2ApiConstants.ROUTER_KEY;
import static org.apache.solr.common.params.CommonParams.ACTION;
import static org.apache.solr.handler.ClusterAPI.wrapParams;
import static org.apache.solr.handler.api.V2ApiUtils.flattenMapWithPrefix;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;

/** All V2 APIs for collection management */
public class CollectionsAPI {

  public static final String V2_RESTORE_CMD = "restore-collection";
  private final CollectionsHandler collectionsHandler;

  public final CollectionsCommands collectionsCommands = new CollectionsCommands();

  public CollectionsAPI(CollectionsHandler collectionsHandler) {
    this.collectionsHandler = collectionsHandler;
  }

  @EndPoint(
      path = {"/c", "/collections"},
      method = POST,
      permission = COLL_EDIT_PERM)
  public class CollectionsCommands {

    @Command(name = V2_RESTORE_CMD)
    @SuppressWarnings("unchecked")
    public void restoreBackup(PayloadObj<RestoreCollectionPayload> obj) throws Exception {
      final RestoreCollectionPayload v2Body = obj.get();
      final Map<String, Object> v1Params = v2Body.toMap(new HashMap<>());

      v1Params.put(ACTION, CollectionAction.RESTORE.toLower());
      if (v2Body.createCollectionParams != null && !v2Body.createCollectionParams.isEmpty()) {
        final Map<String, Object> createCollParams =
            (Map<String, Object>) v1Params.remove(V2ApiConstants.CREATE_COLLECTION_KEY);
        convertV2CreateCollectionMapToV1ParamMap(createCollParams);
        v1Params.putAll(createCollParams);
      }

      collectionsHandler.handleRequestBody(
          wrapParams(obj.getRequest(), v1Params), obj.getResponse());
    }

    @SuppressWarnings("unchecked")
    private void convertV2CreateCollectionMapToV1ParamMap(Map<String, Object> v2MapVals) {
      // Keys are copied so that map can be modified as keys are looped through.
      final Set<String> v2Keys = v2MapVals.keySet().stream().collect(Collectors.toSet());
      for (String key : v2Keys) {
        switch (key) {
          case V2ApiConstants.PROPERTIES_KEY:
            final Map<String, Object> propertiesMap =
                (Map<String, Object>) v2MapVals.remove(V2ApiConstants.PROPERTIES_KEY);
            flattenMapWithPrefix(propertiesMap, v2MapVals, CollectionAdminParams.PROPERTY_PREFIX);
            break;
          case ROUTER_KEY:
            final Map<String, Object> routerProperties =
                (Map<String, Object>) v2MapVals.remove(V2ApiConstants.ROUTER_KEY);
            flattenMapWithPrefix(routerProperties, v2MapVals, CollectionAdminParams.ROUTER_PREFIX);
            break;
          case V2ApiConstants.CONFIG:
            v2MapVals.put(CollectionAdminParams.COLL_CONF, v2MapVals.remove(V2ApiConstants.CONFIG));
            break;
          case V2ApiConstants.SHUFFLE_NODES:
            v2MapVals.put(
                CollectionAdminParams.CREATE_NODE_SET_SHUFFLE_PARAM,
                v2MapVals.remove(V2ApiConstants.SHUFFLE_NODES));
            break;
          case V2ApiConstants.NODE_SET:
            final Object nodeSetValUncast = v2MapVals.remove(V2ApiConstants.NODE_SET);
            if (nodeSetValUncast instanceof String) {
              v2MapVals.put(CollectionAdminParams.CREATE_NODE_SET_PARAM, nodeSetValUncast);
            } else {
              final List<String> nodeSetList = (List<String>) nodeSetValUncast;
              final String nodeSetStr = String.join(",", nodeSetList);
              v2MapVals.put(CollectionAdminParams.CREATE_NODE_SET_PARAM, nodeSetStr);
            }
            break;
          default:
            break;
        }
      }
    }
  }
}
