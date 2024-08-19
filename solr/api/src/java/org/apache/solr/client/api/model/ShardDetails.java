package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ShardDetails {
  @JsonProperty public String range;

  // TODO NOCOMMIT enum?
  @JsonProperty public String state;

  // TODO NOCOMMIT enum?
  @JsonProperty public String health;

  @JsonProperty public Map<String, ReplicaDetails> replicas;
}
