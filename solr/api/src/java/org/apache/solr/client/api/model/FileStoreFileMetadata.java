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
package org.apache.solr.client.api.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileStoreFileMetadata extends FileStoreMetadata {

  @JsonProperty public Long size;
  @JsonProperty public Date timestamp;
  @JsonProperty public String sha512;
  @JsonProperty public List<String> sig;

  public Map<String, Object> extraProperties = new HashMap<>();

  @JsonAnyGetter
  public Map<String, Object> getExtraProperties() {
    return extraProperties;
  }

  @JsonAnySetter
  public void setExtraProperty(String field, Object value) {
    extraProperties.put(field, value);
  }
}
