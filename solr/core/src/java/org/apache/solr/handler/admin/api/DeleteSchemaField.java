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
package org.apache.solr.handler.admin.api;

import jakarta.inject.Inject;
import org.apache.solr.api.JerseyResource;
import org.apache.solr.client.api.endpoint.DeleteSchemaFieldApi;
import org.apache.solr.client.api.model.SolrJerseyResponse;
import org.apache.solr.schema.IndexSchema;

// TODO NOCOMMIT - putting this bit on hold as it'd be really nice for this class to reuse
// SchemaManager, but that class really bakes in what it expects the input format to look like.  I'm
// going to go refactor SchemaManager so it takes in a SolrCore and a set of already parsed
// operations - that'll make it much more re-usable for both v1 and v2
public class DeleteSchemaField extends JerseyResource implements DeleteSchemaFieldApi {

  private final IndexSchema indexSchema;

  @Inject
  public DeleteSchemaField(IndexSchema indexSchema) {
    this.indexSchema = indexSchema;
  }

  @Override
  public SolrJerseyResponse deleteField(String fieldName) {
    return null;
  }

  @Override
  public SolrJerseyResponse deleteDynamicField(String dynamicFieldName) {
    return null;
  }

  @Override
  public SolrJerseyResponse deleteCopyField(String copyFieldName) {
    return null;
  }

  @Override
  public SolrJerseyResponse deleteFieldType(String fieldTypeName) {
    return null;
  }
}
