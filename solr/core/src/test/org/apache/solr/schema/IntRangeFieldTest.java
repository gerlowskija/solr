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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.JSONWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrReturnFields;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link IntRangeField} */
public class IntRangeFieldTest extends SolrTestCaseJ4 {

  private SolrQueryRequest mockRequest;
  private SolrQueryResponse mockResponse;

  @Before
  public void setUpMocks() {
    mockRequest = mock(SolrQueryRequest.class);
    when(mockRequest.getParams()).thenReturn(new ModifiableSolrParams());

    mockResponse = mock(SolrQueryResponse.class);
    when(mockResponse.getReturnFields()).thenReturn(new SolrReturnFields());
  }

  @Test
  public void testGetUninversionType() {
    final var intRangeField = new IntRangeField();
    assertNull(intRangeField.getUninversionType(null));
  }

  @Test
  public void testWriteJson() throws IOException {
    final var intRangeField = new IntRangeField();
    final var fieldName = "myFieldName";

    final var oneD = new IntRange(fieldName, new int[] {1}, new int[] {10});
    final var twoD = new IntRange(fieldName, new int[] {1, 2}, new int[] {10, 20});
    final var threeD = new IntRange(fieldName, new int[] {1, 2, 3}, new int[] {10, 20, 30});

    assertEquals(
        "\"myFieldName\":\"[1 TO 10]\"", writeFieldToString(intRangeField, fieldName, oneD));
    assertEquals(
        "\"myFieldName\":\"[1,2 TO 10,20]\"", writeFieldToString(intRangeField, fieldName, twoD));
    assertEquals(
        "\"myFieldName\":\"[1,2,3 TO 10,20,30]\"",
        writeFieldToString(intRangeField, fieldName, threeD));
  }

  @Test
  public void testGetSortField() {
    final var intRangeField = new IntRangeField();
    final var schemaField = new SchemaField("foo", intRangeField);
    final var exception = expectThrows(SolrException.class, () -> {
              intRangeField.getSortField(schemaField, true);
            });
    assertEquals("Sorting not supported on range field: foo", exception.getMessage());
  }

  private String writeFieldToString(FieldType field, String fieldName, IndexableField value)
      throws IOException {
    final var strCapturer = new StringWriter();
    try (final var jsonWriter = new JSONWriter(strCapturer, mockRequest, mockResponse)) {
      field.write(jsonWriter, fieldName, value);
    }
    return strCapturer.toString();
  }
}
