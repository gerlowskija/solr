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

import static org.apache.solr.common.params.CoreAdminParams.COLLECTION;
import static org.apache.solr.handler.admin.ClusterStatus.INCLUDE_ALL;

import jakarta.inject.Inject;
import org.apache.solr.client.api.endpoint.CollectionDetailsApi;
import org.apache.solr.client.api.model.CollectionDetailsResponse;
import org.apache.solr.client.solrj.JacksonContentWriter;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.admin.ClusterStatus;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/**
 * V2 API for displaying basic information about a single collection.
 *
 * <p>This API (GET /v2/collections/collectionName) is analogous to the v1
 * /admin/collections?action=CLUSTERSTATUS&amp;collection=collectionName command.
 */
public class CollectionStatusAPI extends AdminAPIBase implements CollectionDetailsApi {

  @Inject
  public CollectionStatusAPI(
      CoreContainer coreContainer, SolrQueryRequest req, SolrQueryResponse rsp) {
    super(coreContainer, req, rsp);
  }

  @Override
  public CollectionDetailsResponse getCollectionDetails(String collectionName) throws Exception {
    final CoreContainer coreContainer = fetchAndValidateZooKeeperAwareCoreContainer();

    final var zkStateReader = coreContainer.getZkController().getZkStateReader();
    // 'CLUSTERSTATUS' logic traditionally takes a solrParams directly from user-parameters, but
    // since we're only fetching info about a single collection here, we mock up the 'solrParams' to
    // reflect the single-coll case.
    final var singleCollParams = new ModifiableSolrParams();
    singleCollParams.add(INCLUDE_ALL, "false");
    singleCollParams.add(COLLECTION, collectionName);

    // TODO Push usage of CollectionDetailsResponse POJO down into ClusterStatus.getClusterStatus to
    // avoid NamedList usage.
    final var collectionDetailsRaw = new NamedList<>();
    new ClusterStatus(zkStateReader, singleCollParams).getClusterStatus(collectionDetailsRaw);
    final var collectionDetailsMap = collectionDetailsRaw.asMap();
    return JacksonContentWriter.DEFAULT_MAPPER.convertValue(
        collectionDetailsMap, CollectionDetailsResponse.class);
  }
}
