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

import org.apache.solr.core.PluginBag;
import org.apache.solr.core.SolrCore;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS "application" configurations for Solr's {@link org.apache.solr.core.CoreContainer} and
 * {@link SolrCore} instances
 */
public class JerseyApplications {

    public static class CoreContainerApp extends Application {

        private Set<Class<?>> classes;
        public Set<Class<?>> getClasses() {
            return classes;
        }

        private void register(Class<?> clazz) {
            classes.add(clazz);
        }

        public CoreContainerApp(PluginBag.JerseyMetricsLookupRegistry beanRegistry) {
            super();

            classes = new HashSet<>();
            // Authentication and authorization
            register(SolrRequestAuthorizer.class);

            // Request and response serialization/deserialization
            // TODO: could these be singletons to save per-request object creations?
            register(MessageBodyWriters.JavabinMessageBodyWriter.class);
            register(MessageBodyWriters.XmlMessageBodyWriter.class);
            register(MessageBodyWriters.CsvMessageBodyWriter.class);
            register(SolrJacksonMapper.class);

            // Request lifecycle logic
            register(CatchAllExceptionMapper.class);
            register(RequestMetricHandling.PreRequestMetricsFilter.class);
            register(RequestMetricHandling.PostRequestMetricsFilter.class);
            register(PostRequestDecorationFilter.class);

            // Logging - disabled by default but useful for debugging Jersey execution
            //      setProperties(
            //          Map.of(
            //              "jersey.config.server.tracing.type",
            //              "ALL",
            //              "jersey.config.server.tracing.threshold",
            //              "VERBOSE"));
        }
    }

    public static class SolrCoreApp extends CoreContainerApp {

        public SolrCoreApp(SolrCore solrCore, PluginBag.JerseyMetricsLookupRegistry beanRegistry) {
            super(beanRegistry);

            // Dependency Injection for Jersey resources
            // TODO JEGERLOW See if I can't get the SolrCore injection back in here
        }
    }
}
