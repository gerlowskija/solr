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

package org.apache.solr.api;

import org.apache.solr.jersey.CatchAllExceptionMapper;
import org.apache.solr.jersey.SolrJerseyResponse;
import org.apache.solr.servlet.HttpSolrCall;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import java.util.function.Supplier;

import static org.apache.solr.jersey.RequestContextConstants.SOLR_JERSEY_RESPONSE_KEY;

/**
 * A marker parent type for all Jersey resource classes
 */
public class JerseyResource {

    @Context
    public ContainerRequestContext containerRequestContext;

    /**
     * Create an instance of the {@link SolrJerseyResponse} subclass; registering it with the Jersey request-context upon creation
     *
     * This utility method primarily exists to allow Jersey resources to return error responses that match those
     * returned by Solr's v1 APIs.
     *
     * When a severe-enough exception halts a v1 request, Solr generates a summary of the error and attaches it to the
     * {@link org.apache.solr.response.SolrQueryResponse} given to the request handler.  This SolrQueryResponse may
     * already hold some portion of the normal "success" response for that API.
     *
     * The JAX-RS framework isn't well suited to mimicking responses of this sort, as the "response" from a Jersey resource
     * is its return value (instead of a passed-in value that gets modified).  This utility works around this limitation
     * by attaching the eventual return value of a JerseyResource to the context associated with the Jersey
     * request.  This allows partially-constructed responses to be accessed later in the case of an exception.
     *
     * In order to instantiate arbitrary SolrJerseyResponse subclasses, this utility uses reflection to find and invoke
     * the first (no-arg) constructor for the specified type.  SolrJerseyResponse subclasses without a no-arg constructor
     * can be instantiated and registered using {@link #instantiateJerseyResponse(Supplier)}
     *
     * @param clazz the SolrJerseyResponse class to instantiate and register
     *
     * @see CatchAllExceptionMapper
     * @see HttpSolrCall#call()
     */
    @SuppressWarnings("unchecked")
    protected <T extends SolrJerseyResponse> T instantiateJerseyResponse(Class<T> clazz) {
        return instantiateJerseyResponse(() -> {
            try {
                return (T) clazz.getConstructors()[0].newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Create an instance of the {@link SolrJerseyResponse} subclass; registering it with the Jersey request-context upon creation
     *
     * This utility method primarily exists to allow Jersey resources to return responses, especially error responses,
     * that match some of the particulars of Solr's traditional/v1 APIs.  See the companion method
     * {@link #instantiateJerseyResponse(Class)} for more details.
     *
     * @param instantiator a lambda to create the desired SolrJerseyResponse
     * @see CatchAllExceptionMapper
     * @see HttpSolrCall#call()
     */
    protected <T extends SolrJerseyResponse> T instantiateJerseyResponse(Supplier<T> instantiator) {
        final T instance = instantiator.get();
        containerRequestContext.setProperty(SOLR_JERSEY_RESPONSE_KEY, instance);
        return instance;
    }
}
