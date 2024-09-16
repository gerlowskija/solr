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
package org.apache.solr.client.solrj.impl;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.NamedList;

/**
 * {@link SolrClient} implementation that delegates all method calls to a wrapped SolrClient
 *
 * <p>Users can take ownership of the wrapped SolrClient, so that it is closed on a call to {@link
 * DelegatingSolrClient#close()}, or they can opt to use DelegatingSolrClient as a "thin" wrapper
 * and leave responsibility for cleanup of the wrapped SolrClient to callers
 */
public class DelegatingSolrClient extends SolrClient {

  private final SolrClient delegate;
  private final boolean shouldClose;

  public DelegatingSolrClient(SolrClient delegate, boolean closeDelegateClient) {
    this.delegate = delegate;
    this.shouldClose = closeDelegateClient;
  }

  // TODO - is there any value in having explicit delegation for the many add, query, ping, commit,
  // etc. overrides built into SolrClient.  Currently we only extend 'request' because all other
  // SolrClient methods trickle down to that eventually

  public NamedList<Object> request(final SolrRequest<?> request, String collection)
      throws SolrServerException, IOException {
    return delegate.request(request, collection);
  }

  public CompletableFuture<NamedList<Object>> requestAsync(SolrRequest<?> request, String collection) {
    // TODO This is a bit of a hack to expose the very useful 'requestAsync' methods available on
    // only some SolrClient implementations.  It'd be much cleaner if this async method was either
    // available on SolrClient itself, or attached to a separate interface. Absent that, this code
    // can be improved once the deprecated HttpSolrClient is gone by changing DelegatingSolrClient
    // to extend HttpSolrClientBase so that it gets 'requestAsync' "naturally".
    if (delegate instanceof HttpSolrClientBase) {
      return ((HttpSolrClientBase) delegate).requestAsync(request, collection);
    } else {
      throw new IllegalStateException(
          "DelegatingSolrClient cannot make asynchronous requests if the wrapped 'delegate' ["
              + delegate
              + "] doesn't support them");
    }
  }

  // TODO see comment on requestAsync(SolrRequest, String) above
  public CompletableFuture<NamedList<Object>> requestAsync(SolrRequest<?> request) {
    return requestAsync(request, null);
  }

  public String getDefaultCollection() {
    return delegate.getDefaultCollection();
  }

  public String getBaseURL() {
    return delegate.getBaseURL();
  }

  /**
   * Closes the "wrapped" delegate client if requested on initial creation in {@link
   * DelegatingSolrClient#DelegatingSolrClient(SolrClient, boolean)}
   */
  @Override
  public void close() throws IOException {
    if (shouldClose) {
      delegate.close();
    }
  }
}
