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

package org.apache.solr.core;

import static org.mockito.Mockito.mock;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.handler.admin.ConfigSetsHandler;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.configsets.ListConfigSetsAPI;
import org.apache.solr.jersey.JerseyApplications;
import org.apache.solr.request.SolrRequestHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link PluginBag} */
public class PluginBagTest extends SolrTestCaseJ4 {

  private SolrCore solrCore;
  private CoreContainer coreContainer;

  @BeforeClass
  public static void ensureWorkingMockito() {
    assumeWorkingMockito();
  }

  @Before
  public void initMocks() {
    solrCore = mock(SolrCore.class);
    coreContainer = mock(CoreContainer.class);
  }
}
