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

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.SolrException;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.uninverting.UninvertingReader;
import java.io.IOException;

/**
 * {@link FieldType} implementation for storing and searching ranges of integers
 */
public class IntRangeField extends FieldType {

  // TODO Enforce index-property invariants similar to what AbstractSpatialPrefixTreeFieldType is doing?

  @Override
  public IndexableField createField(SchemaField field, Object value) {
    return null;
//    int intValue =
//        (value instanceof Number)
//            ? ((Number) value).intValue()
//            : Integer.parseInt(value.toString());
//    return new IntPoint(field.getName(), intValue);
  }
  /**
   * Returns null, indicating that uninversion (i.e. field cache) is not supported for this field type.
   */
  @Override
  public UninvertingReader.Type getUninversionType(SchemaField sf) {
    return null;
  }

  @Override
  public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
    writer.writeStr(name, toExternal(f), true);
  }

  @Override
  public SortField getSortField(SchemaField field, boolean top) {
    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Sorting not supported on range field: " + field.getName());
  }

  // TODO This seems like a lot of string-building for what I imagine is a common operation - is there a cheaper, or more efficient way to do this?
  @Override
  public String toExternal(IndexableField f) {
    assert f instanceof IntRange : "Expected IndexableField type of IntRange, but was " + f.getClass().getSimpleName();

    final var intRangeField = (IntRange) f;
    final var dimensions = intRangeField.fieldType().pointDimensionCount() / 2;

    final var builder = new StringBuilder();
    builder.append("[");
    for (int i = 0 ; i < dimensions; i++) {
      builder.append(intRangeField.getMin(i));
      if (i != dimensions - 1) {
        builder.append(",");
      }
    }

    builder.append(" TO ");

    for (int i = 0 ; i < dimensions; i++) {
      builder.append(intRangeField.getMax(i));
      if (i != dimensions - 1) {
        builder.append(",");
      }
    }
    builder.append("]");
    return builder.toString();
  }
}
