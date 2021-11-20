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

package org.apache.solr.vnext;

import com.google.common.collect.Maps;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.admin.ClusterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static org.apache.solr.common.cloud.ZkStateReader.COLLECTION_PROP;

@Path("/cluster")
public class Cluster {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Context
    private HttpServletRequest httpServletRequest;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String alternateClusterstatus(@QueryParam("collection") String collectionName) throws Exception {
        final CoreContainer coreContainer = (CoreContainer) httpServletRequest.getAttribute("org.apache.solr.CoreContainer");

        final Map<String, Object> params = Maps.newHashMap();
        if (collectionName != null) {
            params.put(COLLECTION_PROP, collectionName);
        }

        final NamedList<Object> results = new NamedList<>();

        final ZkController zkController = coreContainer.getZkController();
        final ZkStateReader zkStateReader = zkController.getZkStateReader();
        log.info("Controller is {}, Reader is {}", zkController, zkStateReader);
        new ClusterStatus(zkStateReader, new ZkNodeProps(params)).getClusterStatus(results);

        return results.jsonStr();
    }
}
