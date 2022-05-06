package org.apache.solr.secret.zk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.cloud.acl.SecretCredentialsProvider;
import org.apache.solr.common.cloud.acl.ZkCredentialsInjector;
import org.apache.solr.util.SolrJacksonAnnotationInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

public class AWSSecretManagerCredentialInjector implements ZkCredentialsInjector {
    public static final String SECRET_CREDENTIAL_PROVIDER_CLASS_VM_PARAM =
            "zkSecretCredentialsProvider";
    public static final String SECRET_CREDENTIAL_PROVIDER_SECRET_NAME_VM_PARAM =
            "zkSecretCredentialSecretName";
    private static final String SECRET_CREDENTIAL_PROVIDER_SECRET_NAME_DEFAULT =
            "zkCredentialsSecret";
    private static final String SECRET_MANAGER_REGION = "zkCredentialsAWSSecretRegion";

    private final ObjectMapper mappers =
            SolrJacksonAnnotationInspector.createObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                    .disable(MapperFeature.AUTO_DETECT_FIELDS);


    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private volatile AWSSecretCredentialsProvider.SecretMultiCredentials secretMultiCredentials;
    private final String secretName;
    private final String regionName;

    public AWSSecretManagerCredentialInjector() {
        String secretNameVmParam =
                System.getProperties().getProperty(SECRET_CREDENTIAL_PROVIDER_SECRET_NAME_VM_PARAM);
        secretName =
                !StringUtils.isEmpty(secretNameVmParam)
                        ? secretNameVmParam
                        : SECRET_CREDENTIAL_PROVIDER_SECRET_NAME_DEFAULT;

            regionName = System.getProperties().getProperty(SECRET_MANAGER_REGION);
    }

    @Override
    public List<ZkCredential> getZkCredentials() {
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
    protected AWSSecretCredentialsProvider.SecretMultiCredentials createSecretMultiCredential(String secretName) {
        try {
            String jsonSecret = getSecretValue(secretName);
            return mappers.readValue(jsonSecret, AWSSecretCredentialsProvider.SecretMultiCredentials.class);
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
