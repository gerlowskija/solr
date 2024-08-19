package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class CollectionDetails {

  @JsonProperty public Integer pullReplicas;

  @JsonProperty public String configName;

  @JsonProperty public Integer replicationFactor;

  @JsonProperty public CollectionRouterProperties router;

  @JsonProperty public Integer nrtReplicas;

  @JsonProperty public Integer tlogReplicas;

  @JsonProperty public Map<String, ShardDetails> shards;

  // TODO NOCOMMIT enum?
  @JsonProperty public String health;

  // TODO NOCOMMIT - is this something we should keep/reflect in all API responses that return this
  // object?
  @JsonProperty public Long znodeVersion;

  @JsonProperty public Long creationTimeMillis;
}
