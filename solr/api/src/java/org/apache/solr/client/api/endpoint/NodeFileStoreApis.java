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
package org.apache.solr.client.api.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.solr.client.api.model.ReadFileStoreResponse;
import org.apache.solr.client.api.model.SolrJerseyResponse;

/** V2 API definitions for storing, retrieving, and inspecting filestore contents. */
@Path("/node/files")
public interface NodeFileStoreApis {



    @GET
    @Operation(
            summary = "Retrieve file contents or metadata from the filestore.",
            tags = {"file-store"})
    //@Produces({"application/vnd.apache.solr.raw", MediaType.APPLICATION_JSON})
    @Path("{path:.+}")
    ReadFileStoreResponse readFileStoreEntry(@Parameter(description = "File store path") @PathParam("path") String path,
                                             @QueryParam("sync") Boolean sync,
                                             @QueryParam("getFrom") String getFrom,
                                             @QueryParam("meta") Boolean meta);

    // TODO Better understand the difference between deleteFile and deleteFileLocal and improve docs here.
    @DELETE
    @Operation(
            summary = "Delete a (local) file or directory from the filestore.",
            tags = {"file-store"})
    @Path("{path:.+}")
    SolrJerseyResponse deleteFileLocal(@Parameter(description = "Path to a file or directory within the filestore") @PathParam("path") String path);
}
