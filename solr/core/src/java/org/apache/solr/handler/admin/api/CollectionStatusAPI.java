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
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_READ_PERM;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.jersey.SolrJerseyResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/**
 * V2 API for displaying basic information about a single collection.
 *
 * <p>This API (GET /v2/collections/collectionName) is analogous to the v1
 * /admin/collections?action=CLUSTERSTATUS&amp;collection=collectionName command.
 */
@Path("/collections/{collName}")
public class CollectionStatusAPI extends AdminAPIBase {

  public CollectionStatusAPI(
      CoreContainer coreContainer,
      SolrQueryRequest solrQueryRequest,
      SolrQueryResponse solrQueryResponse) {
    super(coreContainer, solrQueryRequest, solrQueryResponse);
  }

  @GET
  @Produces({"application/json", "application/xml", BINARY_CONTENT_TYPE_V2})
  @PermissionName(COLL_READ_PERM)
  public SolrJerseyResponse foo(
      @PathParam("collName") String collectionName,
      @QueryParam("coreInfo") Boolean coreInfo,
      @QueryParam("segments") Boolean segments,
      @QueryParam("fieldInfo") Boolean fieldInfo,
      @QueryParam("rawSize") Boolean rawSize,
      @QueryParam("rawSizeSummary") Boolean rawSizeSummary,
      @QueryParam("rawSizeDetails") Boolean rawSizeDetails,
      @QueryParam("rawSizeSamplingPercent") Float rawSizeSamplingPercent,
      @QueryParam("sizeInfo") String sizeInfo) {
    final var response = instantiateJerseyResponse(SolrJerseyResponse.class);
    fetchAndValidateZooKeeperAwareCoreContainer();
    recordCollectionForLogAndTracing(collectionName, solrQueryRequest);

    // TODO Reformat inputs and invoke ColStatus
    return response;
  }
}
