package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionDetailsResponse extends SolrJerseyResponse {

  @JsonProperty("cluster")
  public ClusterDetails cluster;
}
