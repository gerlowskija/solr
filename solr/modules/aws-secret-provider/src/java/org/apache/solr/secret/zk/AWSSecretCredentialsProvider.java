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
package org.apache.solr.secret.zk;

import static org.apache.solr.common.cloud.acl.ZkCredentialsInjector.ZkCredential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.cloud.acl.SecretCredentialsProvider;
import org.apache.solr.util.SolrJacksonAnnotationInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

// This class would be functionally replaced in this PR by AWSSecretManagerCredentialInjector
// I've left it around here because I didn't want to muck around with the tests for this proof-of-concept code change
// spun out of a review discussion
/**
 * An implementation of {@link SecretCredentialsProvider} that retrieves Zookeeper credentials from
 * AWS Secret Manager. It expects a secret value in the following format: <code>
 * { "zkCredentials":
 * [
 * {"username": "admin-user", password": "ADMIN-PASSWORD", "perms": "all"},
 * {"username": "readonly-user", "password":  "READONLY-PASSWORD", "perms": "read"}
 * ]
 * }
 * </code>
 */
public class AWSSecretCredentialsProvider implements SecretCredentialsProvider {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ObjectMapper mappers =
      SolrJacksonAnnotationInspector.createObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
          .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
          .disable(MapperFeature.AUTO_DETECT_FIELDS);

  private static final String SECRET_MANAGER_REGION = "zkCredentialsAWSSecretRegion";

  private final String regionName;
  private volatile SecretMultiCredentials secretMultiCredentials;

  public AWSSecretCredentialsProvider() {
    regionName = System.getProperties().getProperty(SECRET_MANAGER_REGION);
  }

  @Override
  public List<ZkCredential> getZkCredentials(String secretName) {
    createSecretMultiCredentialIfNeeded(secretName);
    List<ZkCredential> zkCredentials = secretMultiCredentials.getZkCredentials();
    log.debug(
        "getZkCredentials for secretName: {} --> zkCredentials: {}", secretName, zkCredentials);
    return zkCredentials != null ? zkCredentials : Collections.emptyList();
  }

  private void createSecretMultiCredentialIfNeeded(String secretName) {
    if (secretMultiCredentials == null) {
      synchronized (this) {
        if (secretMultiCredentials == null) {
          secretMultiCredentials = createSecretMultiCredential(secretName);
        }
      }
    }
  }

  /**
   * expects jsonSecret in the following format: { "zkCredentials": [ {"username": "admin-user",
   * "password": "ADMIN-PASSWORD", "perms": "all"}, {"username": "readonly-user", "password":
   * "READONLY-PASSWORD", "perms": "read"} ] }
   *
   * @param secretName the AWS Secret Manager secret name used to store ZK credentials
   * @return secret in SecretMultiCredentials format
   */
  protected SecretMultiCredentials createSecretMultiCredential(String secretName) {
    try {
      String jsonSecret = getSecretValue(secretName);
      return mappers.readValue(jsonSecret, SecretMultiCredentials.class);
    } catch (JsonProcessingException jpe) {
      // fail fast
      throw new IllegalArgumentException(
          "Exception parsing received secret from AWS Secret Manager ", jpe);
    }
  }

  protected String getSecretValue(String secretName) {
    try {
      final SecretsManagerClientBuilder secretsManagerClientBuilder =
          SecretsManagerClient.builder();
      if (!StringUtils.isEmpty(regionName)) {
        secretsManagerClientBuilder.region(Region.of(regionName));
      }
      final SecretsManagerClient secretsClient = secretsManagerClientBuilder.build();
      final GetSecretValueRequest valueRequest =
          GetSecretValueRequest.builder().secretId(secretName).build();
      return secretsClient.getSecretValue(valueRequest).secretString();
    } catch (SecretsManagerException | IllegalArgumentException sme) {
      // fail fast
      throw new IllegalArgumentException(
          "Exception retrieving secret from AWS Secret Manager ", sme);
    }
  }

  static class SecretMultiCredentials {

    private final List<ZkCredential> zkCredentials;

    public SecretMultiCredentials() {
      this(Collections.emptyList());
    }

    public SecretMultiCredentials(List<ZkCredential> zkCredentials) {
      this.zkCredentials = zkCredentials;
    }

    public List<ZkCredential> getZkCredentials() {
      return zkCredentials;
    }

    @Override
    public String toString() {
      return "SecretMultiCredential{" + "zkCredentials=" + zkCredentials + '}';
    }
  }
}
