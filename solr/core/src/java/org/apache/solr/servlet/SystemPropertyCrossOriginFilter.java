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

package org.apache.solr.servlet;

import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;
import java.util.Set;

public class SystemPropertyCrossOriginFilter extends CrossOriginFilter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        final FilterConfig sysPropAwareConfig = new DelegatingFilterConfig(config);
        super.init(sysPropAwareConfig);
    }

    class DelegatingFilterConfig implements FilterConfig {
        private FilterConfig delegate;
        public DelegatingFilterConfig(FilterConfig delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getFilterName() {
            return delegate.getFilterName();
        }

        @Override
        public ServletContext getServletContext() {
            return delegate.getServletContext();
        }

        @Override
        public String getInitParameter(String name) {
            final Set<String> specialCasedKeys = Set.of("allowedOrigins", "allowedOrigins", "allowedMethods");
            if (specialCasedKeys.contains(name)) {
                final String syspropName = "solr.cors" + name;
                if (System.getProperties().containsKey(syspropName)) {
                    return System.getProperty(syspropName);
                }
            }

            return delegate.getInitParameter(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return delegate.getInitParameterNames();
        }
    }
}
