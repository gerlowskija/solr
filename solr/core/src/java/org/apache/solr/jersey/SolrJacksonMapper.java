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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Instant;

import org.apache.solr.common.util.NamedList;

/** Customizes the ObjectMapper settings used for serialization/deserialization in Jersey */
@SuppressWarnings("rawtypes")
@Provider
public class SolrJacksonMapper implements ContextResolver<ObjectMapper> {

  private static final ObjectMapper objectMapper = createObjectMapper();

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return objectMapper;
  }

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private static ObjectMapper createObjectMapper() {
    final SimpleModule customTypeModule = new SimpleModule();
    customTypeModule.addSerializer(new NamedListSerializer(NamedList.class));
    customTypeModule.addSerializer(new InstantSerializer(Instant.class));
    customTypeModule.addDeserializer(Instant.class, new InstantDeserializer(Instant.class));

    return new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(customTypeModule);
  }

  public static class NamedListSerializer extends StdSerializer<NamedList> {

    public NamedListSerializer() {
      this(null);
    }

    public NamedListSerializer(Class<NamedList> nlClazz) {
      super(nlClazz);
    }

    @Override
    public void serialize(NamedList value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeObject(value.asShallowMap());
    }
  }

  public static class InstantSerializer extends StdSerializer<Instant> {
    public InstantSerializer(Class<Instant> nlClazz) { super(nlClazz); }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeObject(value.toString());
    }
  }

  public static class InstantDeserializer extends StdDeserializer<Instant> {

    public InstantDeserializer(Class<Instant> instantClazz) {
      super(instantClazz);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
      return Instant.parse(p.getText());
    }
  }
}
