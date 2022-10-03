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

package org.apache.solr.handler.admin.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Unit tests for {@link ListCollectionsAPI}. */
public class ListCollectionsAPITest extends SolrTestCaseJ4 {

  private CoreContainer mockCoreContainer;
  private SolrQueryRequest mockQueryRequest;
  private SolrQueryResponse queryResponse;
  private ListCollectionsAPI listCollectionsAPI;

  @BeforeClass
  public static void ensureWorkingMockito() {
    assumeWorkingMockito();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    mockCoreContainer = mock(CoreContainer.class);
    mockQueryRequest = mock(SolrQueryRequest.class);
    queryResponse = new SolrQueryResponse();

    listCollectionsAPI = new ListCollectionsAPI(mockCoreContainer, mockQueryRequest, queryResponse);
  }

  @Test
  public void testReportsErrorWhenCalledInStandaloneMode() {
    when(mockCoreContainer.isZooKeeperAware()).thenReturn(false);

    final SolrException e =
        expectThrows(SolrException.class, () -> listCollectionsAPI.listCollections());
    assertEquals(400, e.code());
    assertTrue(
        "Exception message differed from expected: " + e.getMessage(),
        e.getMessage().contains("not running in SolrCloud mode"));
  }
}
