/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import java.util.Map;

/**
 * CoreContainer-level (i.e. ADMIN) Jersey API registration.
 */
public class CoreContainerApp extends ResourceConfig {

    public CoreContainerApp() {
        super();
        setProperties(Map.of("jersey.config.server.tracing.type", "ALL", "jersey.config.server.tracing.threshold", "VERBOSE"));
        register(ApplicationEventLogger.class);
        register(RequestEventLogger.class);
        register(SolrRequestAuthorizer.class);
        //register(RequestMetricsInitializer.class);
        register(JavabinWriter.class);
        register(CatchAllExceptionMapper.class);

        // Individual Jersey APIs only need to be registered here if they're not affiliated with a request handler
    }
}
