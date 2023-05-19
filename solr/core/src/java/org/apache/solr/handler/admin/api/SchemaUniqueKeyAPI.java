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

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.solr.api.JerseyResource;
import org.apache.solr.core.SolrCore;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.jersey.SolrJerseyResponse;
import org.apache.solr.security.PermissionNameProvider;

/**
 * V2 API for getting the name of the unique-key field for an in-use schema.
 *
 * <p>This API (GET /api/collections/collectionName/schema/uniquekey) is analogous to the v1
 * /solr/collectionName/schema/uniquekey API.
 */
public class SchemaUniqueKeyAPI extends JerseyResource {

  private SolrCore solrCore;

  @Inject
  public SchemaUniqueKeyAPI(SolrCore solrCore) {
    this.solrCore = solrCore;
  }

  @GET
  @Path("/schema/uniquekey")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML, BINARY_CONTENT_TYPE_V2})
  @PermissionName(PermissionNameProvider.Name.SCHEMA_READ_PERM)
  public SchemaUniqueKeyResponse getSchemaUniqueKey() {
    SchemaUniqueKeyResponse response = instantiateJerseyResponse(SchemaUniqueKeyResponse.class);

    response.uniqueKey = solrCore.getLatestSchema().getUniqueKeyField().getName();

    return response;
  }

  public static class SchemaUniqueKeyResponse extends SolrJerseyResponse {
    @JsonProperty("uniqueKey")
    public String uniqueKey;
  }
}
