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

import org.apache.solr.client.solrj.SolrClient;

/**
 * {@link DelegatingSolrClient} that overrides the delegate's "base URL" with a replacement value.
 *
 * <p>Primarily used to allow existing "HTTP" SolrClients to hit an alternate Solr server on a
 * temporary or per-request basis.
 */
public class URLReplacingSolrClient extends DelegatingSolrClient {

  private final String replacementBaseUrl;

  /**
   * Wraps the delegate in a "URL Replacing" instance.
   *
   * <p>Caller retains ownership of {@code delegate} and is responsible for closing it.
   *
   * @param baseUrl a Solr base URL, of the form "scheme://host:port/solr" or
   *     "scheme://host:port/api"
   * @param delegate an existing SolrClient instance whose base URL should be masked/replaced.
   */
  public URLReplacingSolrClient(String baseUrl, SolrClient delegate) {
    super(delegate, false);

    this.replacementBaseUrl = baseUrl;
  }

  /**
   * Wraps the delegate in a "URL Replacing" instance.
   *
   * <p>Caller retains ownership of {@code delegate} and is responsible for closing it if {@code
   * shouldCloseDelegate} is 'false'
   *
   * @param baseUrl a Solr base URL, of the form "scheme://host:port/solr" or
   *     "scheme://host:port/api"
   * @param delegate an existing SolrClient instance whose base URL should be masked/replaced.
   * @param shouldCloseDelegate 'true' if this instance should "own" the delegate client and be
   *     responsible for closing it, 'false' if that responsibility will be handled by the caller.
   */
  public URLReplacingSolrClient(String baseUrl, SolrClient delegate, boolean shouldCloseDelegate) {
    super(delegate, shouldCloseDelegate);

    this.replacementBaseUrl = baseUrl;
  }

  @Override
  public String getBaseURL() {
    return replacementBaseUrl;
  }
}
