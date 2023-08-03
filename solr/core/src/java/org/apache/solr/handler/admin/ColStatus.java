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

package org.apache.solr.handler.admin;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.RoutingRule;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkCoreNodeProps;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.api.V2ApiUtils;
import org.apache.solr.jersey.JacksonReflectMapWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Report low-level details of collection. */
public class ColStatus {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ClusterState clusterState;
  private final ZkNodeProps props;
  private final SolrClientCache solrClientCache;

  public static final String CORE_INFO_PROP = SegmentsInfoRequestHandler.CORE_INFO_PARAM;
  public static final String FIELD_INFO_PROP = SegmentsInfoRequestHandler.FIELD_INFO_PARAM;
  public static final String SIZE_INFO_PROP = SegmentsInfoRequestHandler.SIZE_INFO_PARAM;
  public static final String RAW_SIZE_PROP = SegmentsInfoRequestHandler.RAW_SIZE_PARAM;
  public static final String RAW_SIZE_SUMMARY_PROP =
      SegmentsInfoRequestHandler.RAW_SIZE_SUMMARY_PARAM;
  public static final String RAW_SIZE_DETAILS_PROP =
      SegmentsInfoRequestHandler.RAW_SIZE_DETAILS_PARAM;
  public static final String RAW_SIZE_SAMPLING_PERCENT_PROP =
      SegmentsInfoRequestHandler.RAW_SIZE_SAMPLING_PERCENT_PARAM;
  public static final String SEGMENTS_PROP = "segments";

  public ColStatus(SolrClientCache solrClientCache, ClusterState clusterState, ZkNodeProps props) {
    this.props = props;
    this.solrClientCache = solrClientCache;
    this.clusterState = clusterState;
  }

  public static class CollectionStatusSummary implements JacksonReflectMapWriter {
    @JsonProperty public Integer znodeVersion;

    @JsonProperty
    public Map<String, Object>
        properties; // 'Object' necessary both because of integer props (e.g. nrtReplicas) and
    // compound props (e.g. router)

    @JsonProperty public Integer activeShards;
    @JsonProperty public Integer inactiveShards;
    @JsonProperty public Set<String> schemaNonCompliant;
    @JsonProperty public Map<String, ShardSummary> shards;
  }

  public static class ShardSummary implements JacksonReflectMapWriter {
    @JsonProperty public String state;
    @JsonProperty public String range;
    @JsonProperty public Map<String, RoutingRule> routingRules;
    @JsonProperty public ReplicaStateSummary replicas;
    @JsonProperty public LeaderSummary leader;
  }

  public static class ReplicaStateSummary implements JacksonReflectMapWriter {
    @JsonProperty public Integer total;
    @JsonProperty public Integer active;
    @JsonProperty public Integer down;
    @JsonProperty public Integer recovering;
    @JsonProperty public Integer recovery_failed;
  }

  public static class LeaderSummary implements JacksonReflectMapWriter {
    @JsonProperty public String coreNode;
    @JsonProperty public String core;
    @JsonProperty public String leader; // TODO Should this be a Boolean?

    @JsonProperty("node_name")
    public String nodeName;

    @JsonProperty("base_url")
    public String baseUrl;

    @JsonProperty public String state;
    @JsonProperty public String type;

    @JsonProperty("force_set_state")
    public String forceSetState;
    //    @JsonProperty public Map<String, SegmentInfoSummary> segInfos;
    @JsonProperty public Map<String, Object> segInfos;

    private Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> unknownProperties() {
      return unknownFields;
    }

    @JsonAnySetter
    public void setUnknownProperty(String field, Object value) {
      unknownFields.put(field, value);
    }
  }

  //  public static class SegmentInfoSummary implements JacksonReflectMapWriter {
  //    @JsonProperty public String commitLuceneVersion;
  //    @JsonProperty public Integer numSegments;
  //    @JsonProperty public String segmentsFileName;
  //    @JsonProperty public Integer totalMaxDoc; // Long?
  //    @JsonProperty public Object userData; // NOCOMMIT: What is this supposed to hold?
  //  }

  public Map<String, CollectionStatusSummary> getColStatus() {
    Collection<String> collections;
    String col = props.getStr(ZkStateReader.COLLECTION_PROP);
    if (col == null) {
      collections = new HashSet<>(clusterState.getCollectionStates().keySet());
    } else {
      collections = Collections.singleton(col);
    }
    boolean withFieldInfo = props.getBool(FIELD_INFO_PROP, false);
    boolean withSegments = props.getBool(SEGMENTS_PROP, false);
    boolean withCoreInfo = props.getBool(CORE_INFO_PROP, false);
    boolean withSizeInfo = props.getBool(SIZE_INFO_PROP, false);
    boolean withRawSizeInfo = props.getBool(RAW_SIZE_PROP, false);
    boolean withRawSizeSummary = props.getBool(RAW_SIZE_SUMMARY_PROP, false);
    boolean withRawSizeDetails = props.getBool(RAW_SIZE_DETAILS_PROP, false);
    Object samplingPercentVal = props.get(RAW_SIZE_SAMPLING_PERCENT_PROP);
    Float samplingPercent =
        samplingPercentVal != null ? Float.parseFloat(String.valueOf(samplingPercentVal)) : null;
    if (withRawSizeSummary || withRawSizeDetails) {
      withRawSizeInfo = true;
    }
    if (withFieldInfo || withSizeInfo) {
      withSegments = true;
    }

    final Map<String, CollectionStatusSummary> multiCollectionSummary = new HashMap<>();
    for (String collection : collections) {
      DocCollection coll = clusterState.getCollectionOrNull(collection);
      if (coll == null) {
        continue;
      }
      final var colMap = new CollectionStatusSummary();
      colMap.znodeVersion = coll.getZNodeVersion();
      Map<String, Object> props = new TreeMap<>(coll.getProperties());
      props.remove("shards");
      colMap.properties = props;
      colMap.activeShards = coll.getActiveSlices().size();
      colMap.inactiveShards = coll.getSlices().size() - coll.getActiveSlices().size();
      multiCollectionSummary.put(collection, colMap);

      Set<String> nonCompliant = new TreeSet<>();

      colMap.shards = new HashMap<>();
      for (Slice s : coll.getSlices()) {
        final var sliceMap = new ShardSummary();
        colMap.shards.put(s.getName(), sliceMap);
        final var replicaMap = new ReplicaStateSummary();
        int totalReplicas = s.getReplicas().size();
        int activeReplicas = 0;
        int downReplicas = 0;
        int recoveringReplicas = 0;
        int recoveryFailedReplicas = 0;
        for (Replica r : s.getReplicas()) {
          // replica may still be marked as ACTIVE even though its node is no longer live
          if (!r.isActive(clusterState.getLiveNodes())) {
            downReplicas++;
            continue;
          }
          switch (r.getState()) {
            case ACTIVE:
              activeReplicas++;
              break;
            case DOWN:
              downReplicas++;
              break;
            case RECOVERING:
              recoveringReplicas++;
              break;
            case RECOVERY_FAILED:
              recoveryFailedReplicas++;
              break;
          }
        }
        replicaMap.total = totalReplicas;
        replicaMap.active = activeReplicas;
        replicaMap.down = downReplicas;
        replicaMap.recovering = recoveringReplicas;
        replicaMap.recovery_failed = recoveryFailedReplicas;
        sliceMap.state = s.getState().toString();
        if (s.getRange() != null) {
          sliceMap.range = s.getRange().toString();
        }
        // TODO NOCOMMIT JEGERLOW - RoutingRule still needs Jackson-ified
        Map<String, RoutingRule> rules = s.getRoutingRules();
        if (rules != null && !rules.isEmpty()) {
          sliceMap.routingRules = rules;
        }
        sliceMap.replicas = replicaMap;
        Replica leader = s.getLeader();
        if (leader == null) { // pick the first one
          leader = s.getReplicas().size() > 0 ? s.getReplicas().iterator().next() : null;
        }
        if (leader == null) {
          continue;
        }
        final var leaderMap = new LeaderSummary();
        sliceMap.leader = leaderMap;
        leaderMap.coreNode = leader.getName();
        leaderMap.unknownProperties().putAll(leader.getProperties());
        if (!leader.isActive(clusterState.getLiveNodes())) {
          continue;
        }
        String url = ZkCoreNodeProps.getCoreUrl(leader);
        if (url == null) {
          continue;
        }
        try (SolrClient client = solrClientCache.getHttpSolrClient(url)) {
          ModifiableSolrParams params = new ModifiableSolrParams();
          params.add(CommonParams.QT, "/admin/segments");
          params.add(FIELD_INFO_PROP, "true");
          params.add(CORE_INFO_PROP, String.valueOf(withCoreInfo));
          params.add(SIZE_INFO_PROP, String.valueOf(withSizeInfo));
          params.add(RAW_SIZE_PROP, String.valueOf(withRawSizeInfo));
          params.add(RAW_SIZE_SUMMARY_PROP, String.valueOf(withRawSizeSummary));
          params.add(RAW_SIZE_DETAILS_PROP, String.valueOf(withRawSizeDetails));
          if (samplingPercent != null) {
            params.add(RAW_SIZE_SAMPLING_PERCENT_PROP, String.valueOf(samplingPercent));
          }
          QueryRequest req = new QueryRequest(params);
          NamedList<Object> rsp = client.request(req);
          rsp.remove("responseHeader");
          // TODO NOCOMMIT JEGERLOW - this is about where I'm petering out for the day.  I've done a
          //  decent job of representing the colstatus response, except for this last part where we
          //  take the response from an /admin/segments call and attach a trimmed down copy of it to
          //  the structured response here.  Obviously this is difficult to do at this point.  My
          // next
          //  step is to do the structured JAX-RS response thing for /admin/segments, and then I can
          //  find a way to convert the NamedList here into that strongly typed response.
          // (ObjectMapper?
          //  V2ApiUtils?)  Should I put this whole PR on hold until /admin/segments is done?
          // Should
          //  I do both APIs in the same PR?  Or should I do the bare minimum /admin/segments thing
          // here
          //  and just model the response for usage here?
          leaderMap.segInfos = rsp;
          NamedList<?> segs = (NamedList<?>) rsp.get("segments");
          if (segs != null) {
            for (Map.Entry<String, ?> entry : segs) {
              NamedList<Object> fields =
                  (NamedList<Object>) ((NamedList<Object>) entry.getValue()).get("fields");
              if (fields != null) {
                for (Map.Entry<String, Object> fEntry : fields) {
                  Object nc = ((NamedList<Object>) fEntry.getValue()).get("nonCompliant");
                  if (nc != null) {
                    nonCompliant.add(fEntry.getKey());
                  }
                }
              }
              if (!withFieldInfo) {
                ((NamedList<Object>) entry.getValue()).remove("fields");
              }
            }
          }
          if (!withSegments) {
            rsp.remove("segments");
          }
          if (!withFieldInfo) {
            rsp.remove("fieldInfoLegend");
          }
        } catch (SolrServerException | IOException e) {
          log.warn("Error getting details of replica segments from {}", url, e);
        }
      }
      if (nonCompliant.isEmpty()) {
        nonCompliant.add("(NONE)");
      }
      colMap.schemaNonCompliant = nonCompliant;
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  public void getColStatus(NamedList<Object> results) {
    final var summary = getColStatus();
    V2ApiUtils.squashIntoNamedListWithoutHeader(results, summary);
  }
}
