package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReplicaDetails {
  @JsonProperty public String core;

  @JsonProperty("node_name")
  public String nodeName;

  // TODO NOCOMMIT enum?
  @JsonProperty public String type;

  // TODO NOCOMMIT enum?
  @JsonProperty public String state;

  @JsonProperty public Boolean leader;

  @JsonProperty("force_set_state")
  public Boolean forceSetState;

  @JsonProperty("base_url")
  public String baseUrl;
}
