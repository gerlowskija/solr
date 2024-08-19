package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ClusterDetails {

  @JsonProperty("live_nodes")
  public List<String> liveNodes;

  @JsonProperty("collections")
  public Map<String, CollectionDetails> collections;

  // TODO NOCOMMIT 'properties', 'roles'

}
