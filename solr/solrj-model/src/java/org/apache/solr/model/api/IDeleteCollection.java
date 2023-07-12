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

package org.apache.solr.model.api;

import org.apache.solr.model.api.response.SubResponseAccumulatingJerseyResponse;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static org.apache.solr.model.api.Constants.BINARY_CONTENT_TYPE_V2;

@Path("/collections/{collectionName}")
public interface IDeleteCollection {

    @DELETE
    @Produces({"application/json", "application/xml", BINARY_CONTENT_TYPE_V2})
    SubResponseAccumulatingJerseyResponse deleteCollection(@PathParam("collectionName") String collectionName, @QueryParam("followAliases") Boolean followAliases, @QueryParam("async") String asyncId) throws Exception;
}
