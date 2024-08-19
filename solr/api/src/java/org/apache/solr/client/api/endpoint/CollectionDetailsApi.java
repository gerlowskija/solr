package org.apache.solr.client.api.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.apache.solr.client.api.model.CollectionDetailsResponse;

@Path("/collections/{collectionName}")
public interface CollectionDetailsApi {

  @GET
  @Operation(
      summary = "Fetch metadata and topology information about the specified collection",
      tags = {"collections"})
  CollectionDetailsResponse getCollectionDetails(
      @Parameter(description = "The collection to return information about.")
          @PathParam("collectionName")
          String collectionName)
      throws Exception;
}
