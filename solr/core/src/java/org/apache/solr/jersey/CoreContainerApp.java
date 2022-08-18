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

import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.configsets.ListConfigSetsAPI;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * CoreContainer-level (i.e. ADMIN) Jersey API registration.
 */
public class CoreContainerApp extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CoreContainerApp(CoreContainer coreContainer) {
        super();
        setProperties(Map.of("jersey.config.server.tracing.type", "ALL", "jersey.config.server.tracing.threshold", "VERBOSE"));
        register(SolrRequestAuthorizer.class);
        register(JavabinWriter.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new CoreContainerFactory(coreContainer))
                        .to(CoreContainer.class)
                        .in(Singleton.class);
            }
        });
        // Register individual APIs (or maybe we should just scan with a 'packages' call?)
        register(ListConfigSetsAPI.class);
        //packages(true, "org.apache.solr.jersey.container");
    }
}
