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

import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.servlet.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.lang.invoke.MethodHandles;

import static org.apache.solr.jersey.RequestContextConstants.HANDLER_METRICS_KEY;
import static org.apache.solr.jersey.RequestContextConstants.SOLR_JERSEY_RESPONSE_KEY;
import static org.apache.solr.jersey.RequestContextConstants.SOLR_QUERY_REQUEST_KEY;

/**
 * Flattens the exception an sets on a {@link SolrJerseyResponse}.
 *
 * <p>Format and behavior based on the exception handling in Solr's v1 requestHandler's. Also sets
 * metrics if present on the request context.
 */
public class CatchAllExceptionMapper implements ExceptionMapper<Exception> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Context public ContainerRequestContext containerRequestContext;

  @Override
  public Response toResponse(Exception exception) {
    // Exceptions coming from the JAX-RS framework itself should be handled separately.
    if (exception instanceof WebApplicationException) {
      return processWebApplicationException((WebApplicationException) exception);
    }

    final SolrQueryRequest solrQueryRequest =
        (SolrQueryRequest) containerRequestContext.getProperty(SOLR_QUERY_REQUEST_KEY);

    // First, handle any exception-related metrics
    final Exception normalizedException =
        RequestHandlerBase.normalizeReceivedException(solrQueryRequest, exception);
    final RequestHandlerBase.HandlerMetrics metrics =
        (RequestHandlerBase.HandlerMetrics)
            containerRequestContext.getProperty(HANDLER_METRICS_KEY);
    if (metrics != null) {
      RequestHandlerBase.processErrorMetricsOnException(normalizedException, metrics);
    }

    // Then, actually convert this to a response
    final SolrJerseyResponse response =
        (SolrJerseyResponse) containerRequestContext.getProperty(SOLR_JERSEY_RESPONSE_KEY);
    response.error = ResponseUtils.getTypedErrorInfo(normalizedException, log);
    ;
    response.responseHeader.status = response.error.code;
    return Response.status(response.error.code).entity(response).build();
  }

  private Response processWebApplicationException(WebApplicationException wae) {
    return wae.getResponse();
  }
}
