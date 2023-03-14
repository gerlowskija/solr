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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.jersey.JacksonReflectMapWriter;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.jersey.SolrJerseyResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

import static org.apache.solr.client.solrj.impl.BinaryResponseParser.BINARY_CONTENT_TYPE_V2;
import static org.apache.solr.cloud.api.collections.CollectionHandlingUtils.NUM_SLICES;
import static org.apache.solr.cloud.api.collections.CollectionHandlingUtils.SHARDS_PROP;
import static org.apache.solr.common.params.CollectionAdminParams.ALIAS;
import static org.apache.solr.common.params.CollectionAdminParams.NRT_REPLICAS;
import static org.apache.solr.common.params.CollectionAdminParams.PER_REPLICA_STATE;
import static org.apache.solr.common.params.CollectionAdminParams.PULL_REPLICAS;
import static org.apache.solr.common.params.CollectionAdminParams.REPLICATION_FACTOR;
import static org.apache.solr.common.params.CollectionAdminParams.TLOG_REPLICAS;
import static org.apache.solr.common.params.CommonAdminParams.ASYNC;
import static org.apache.solr.common.params.CommonAdminParams.WAIT_FOR_FINAL_STATE;
import static org.apache.solr.common.params.CommonParams.NAME;
import static org.apache.solr.common.params.CoreAdminParams.CONFIG;
import static org.apache.solr.security.PermissionNameProvider.Name.COLL_EDIT_PERM;

@Path("/aliases")
public class CreateAliasAPI extends AdminAPIBase {
    @Inject
    public CreateAliasAPI(CoreContainer coreContainer, SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse) {
        super(coreContainer, solrQueryRequest, solrQueryResponse);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, BINARY_CONTENT_TYPE_V2})
    @PermissionName(COLL_EDIT_PERM)
    public SolrJerseyResponse createAlias(CreateAliasRequestBody requestBody) {
        final SolrJerseyResponse response = instantiateJerseyResponse(SolrJerseyResponse.class);
        return response;
    }

    public static class CreateAliasRequestBody implements JacksonReflectMapWriter {
        @JsonProperty(required = true)
        public String name;

        // TODO v1 takes this in as a comma-delimited string
        @JsonProperty("collections")
        public List<String> collections;

        @JsonProperty(ASYNC)
        public String async;

        @JsonProperty("router")
        public AliasRouterInfo router;

        @JsonProperty("create-collection")
        public CreateCollectionRequestBody collCreationParameters;
    }

    public static class AliasRouterInfo implements JacksonReflectMapWriter {
        @JsonProperty(NAME)
        public String name;

        // Used for non-dimensional aliases
        @JsonProperty("properties")
        public RoutedAliasProperties properties;

        // Used for dimensional aliases - mutually exclusive with 'properties' above
        @JsonProperty("routerList")
        public List<RoutedAliasProperties> routerList;
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TimeRoutedAliasProperties.class, name = "time"),
            @JsonSubTypes.Type(value = CategoryRoutedAliasProperties.class, name = "category")
    })
    public static class RoutedAliasProperties implements JacksonReflectMapWriter {
        @JsonProperty("field")
        public String field;
    }

    public static class TimeRoutedAliasProperties extends RoutedAliasProperties {
        // Expected to be a date/time in ISO format, or 'NOW'
        @JsonProperty("start")
        public String start;

        // TODO Change this to 'timezone' or something less abbreviated
        @JsonProperty("tz")
        public String tz;

        @JsonProperty("interval")
        public String interval;

        @JsonProperty("maxFutureMs")
        public Long maxFutureMs;

        @JsonProperty("preemptiveCreateMath")
        public String preemptiveCreateMath;

        @JsonProperty("autoDeleteAge")
        public String autoDeleteAge;
    }

    public static class CategoryRoutedAliasProperties extends RoutedAliasProperties {
        @JsonProperty("maxCardinality")
        public String maxCardinality;

        @JsonProperty("mustMatch")
        public String mustMatch;
    }

    // TODO Move this to CreateCollectionAPI when that is created.
    public static class CreateCollectionRequestBody implements JacksonReflectMapWriter {
        @JsonProperty(NAME)
        public String name;

        @JsonProperty(REPLICATION_FACTOR)
        public Integer replicationFactor;

        @JsonProperty(CONFIG)
        public String config;

        @JsonProperty(NUM_SLICES)
        public Integer numShards;

        @JsonProperty("nodeSet") // V1 API uses 'createNodeSet'
        public List<String> nodeSet;

        @JsonProperty("shuffleNodes") // V1 API uses 'createNodeSet.shuffle'
        public Boolean shuffleNodes;

        // TODO This is currently a comma-separate list of shard names.  We should change it to be an actual List<String> instead, and maybe rename to 'shardNames' or something similar
        @JsonProperty(SHARDS_PROP)
        public String shards;

        @JsonProperty(PULL_REPLICAS)
        public Integer pullReplicas;

        @JsonProperty(TLOG_REPLICAS)
        public Integer tlogReplicas;

        @JsonProperty(NRT_REPLICAS)
        public Integer nrtReplicas;

        @JsonProperty(WAIT_FOR_FINAL_STATE)
        public Boolean waitForFinalState;

        @JsonProperty(PER_REPLICA_STATE)
        public Boolean perReplicaState;

        @JsonProperty(ALIAS)
        public String alias; // the name of an alias to create in addition to the collection

        @JsonProperty("properties")
        public Map<String, String> properties;

        @JsonProperty(ASYNC)
        public String async;

        @JsonProperty("router")
        public CollectionRouterProperties router;
    }

    public static class CollectionRouterProperties implements JacksonReflectMapWriter {
        @JsonProperty(NAME)
        public String name;

        @JsonProperty("field")
        public String field;
    }
}
