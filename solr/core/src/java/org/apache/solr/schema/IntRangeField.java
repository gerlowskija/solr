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
package org.apache.solr.schema;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.uninverting.UninvertingReader;
import java.io.IOException;

/**
 * {@link FieldType} implementation for storing and searching ranges of integers
 */
public class IntRangeField extends FieldType {

  // TODO Enforce index-property invariants similar to what AbstractSpatialPrefixTreeFieldType is doing?

  /**
   * Returns null, indicating that uninversion (i.e. field cache) is not supported for this field type.
   */
  @Override
  public UninvertingReader.Type getUninversionType(SchemaField sf) {
    return null;
  }

  @Override
  public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
    // TODO Idk how to implement this without knowing how users would want to represent their ranges
  }

  @Override
  public SortField getSortField(SchemaField field, boolean top) {
    return null;
  }
}
