package org.apache.solr.client.solrj;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttp2SolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.impl.LBSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionsApi;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.GenericV2SolrRequest;
import org.apache.solr.client.solrj.request.V2Request;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests validating that all {@link SolrClient} implementations make both v1 and v2 requests without
 * special setup.
 *
 * <p>v1 and v2 APIs are very different from one another, even on the client side. The tests in this
 * class are meant to ensure that all SolrClient implementations are capable of sending both v1 and
 * v2 requests successfully. These tests are written partially in response to bugs like those
 * described in the comments of SOLR-15735
 */
public class SolrClientApiVersionInteroperabilityTest extends SolrCloudTestCase {
  private static final String COLL_NAME = "testCollection";
  private static final String CONF_NAME = "conf";

  private String solrV1Url;
  private String solrV2Url;
  private String solrRootUrl;
  private String zkHost;
  private String zkChroot;

  @BeforeClass
  public static void setupCluster() throws Exception {
    configureCluster(2)
        .addConfig(
            "conf",
            getFile("solrj")
                .toPath()
                .resolve("solr")
                .resolve("configsets")
                .resolve("streaming")
                .resolve("conf"))
        .configure();

    cluster.waitForAllNodes(30);

    final var createReq = CollectionAdminRequest.createCollection(COLL_NAME, CONF_NAME, 2, 1);
    final var state = createReq.processAndWait(cluster.getSolrClient(), 30);
    assertEquals(RequestStatusState.COMPLETED, state);
  }

  @Before
  public void getClusterCoordinates() {
    zkHost = cluster.getZkServer().getZkHost();
    zkChroot = "/solr";

    // URLs to the "API root" path for each respective API version
    solrV1Url = cluster.getRandomJetty(random()).getBaseUrl().toString();
    solrV2Url = cluster.getRandomJetty(random()).getBaseURLV2().toString();

    // Derive Solr's "node root" path (i.e. a URL without either the v1 or v2 API path segment)
    final var v1ApiRootSegment = SolrRequest.ApiVersion.V1.getApiPrefix();
    assertTrue(solrV1Url.endsWith(v1ApiRootSegment));
    solrRootUrl = solrV1Url.substring(0, solrV1Url.length() - v1ApiRootSegment.length());
  }

  @Test
  public void testCanMakeV1AndV2Requests_HttpSolrClient() throws SolrServerException, IOException {
    try (final var v1RootClient = new HttpSolrClient.Builder(solrV1Url).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }

    try (final var v2RootClient = new HttpSolrClient.Builder(solrV2Url).build()) {
      makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
    }

    try (final var rootClient = new HttpSolrClient.Builder(solrRootUrl).build()) {
      makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_Http2SolrClient() throws SolrServerException, IOException {
    try (final var v1RootClient = new Http2SolrClient.Builder(solrV1Url).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }

    try (final var v2RootClient = new Http2SolrClient.Builder(solrV2Url).build()) {
      makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
    }

    try (final var rootClient = new Http2SolrClient.Builder(solrRootUrl).build()) {
      makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_ConcurrentUpdateSolrClient()
      throws SolrServerException, IOException {
    try (final var v1RootClient = new ConcurrentUpdateSolrClient.Builder(solrV1Url).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }

    try (final var v2RootClient = new ConcurrentUpdateSolrClient.Builder(solrV2Url).build()) {
      makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
    }

    try (final var rootClient = new ConcurrentUpdateSolrClient.Builder(solrRootUrl).build()) {
      makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_ConcurrentUpdateHttp2SolrClient()
      throws SolrServerException, IOException {
    try (final var underlyingClient = new Http2SolrClient.Builder(solrV1Url).build()) {
      try (final var v1RootClient =
          new ConcurrentUpdateHttp2SolrClient.Builder(solrV1Url, underlyingClient).build()) {
        makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
      }

      try (final var v2RootClient =
          new ConcurrentUpdateHttp2SolrClient.Builder(solrV2Url, underlyingClient).build()) {
        makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
      }

      try (final var rootClient =
          new ConcurrentUpdateHttp2SolrClient.Builder(solrRootUrl, underlyingClient).build()) {
        makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
      }
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_LBHttpSolrClient()
      throws SolrServerException, IOException {
    try (final var underlyingClient = new Http2SolrClient.Builder(solrV1Url).build()) {
      try (final var v1RootClient =
          new LBHttp2SolrClient.Builder(underlyingClient, LBSolrClient.Endpoint.from(solrV1Url))
              .build()) {
        makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
      }

      try (final var v2RootClient =
          new LBHttp2SolrClient.Builder(underlyingClient, LBSolrClient.Endpoint.from(solrV2Url))
              .build()) {
        makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
      }

      try (final var rootClient =
          new LBHttp2SolrClient.Builder(underlyingClient, LBSolrClient.Endpoint.from(solrRootUrl))
              .build()) {
        makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
      }
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_LBHttp2SolrClient()
      throws SolrServerException, IOException {
    try (final var v1RootClient =
        new LBHttpSolrClient.Builder().withBaseEndpoint(solrV1Url).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }

    try (final var v2RootClient =
        new LBHttpSolrClient.Builder().withBaseEndpoint(solrV2Url).build()) {
      makeV1AndV2ListCollectionRequests(v2RootClient, COLL_NAME);
    }

    try (final var rootClient =
        new LBHttpSolrClient.Builder().withBaseEndpoint(solrRootUrl).build()) {
      makeV1AndV2ListCollectionRequests(rootClient, COLL_NAME);
    }
  }

  // TODO LB client

  @Test
  public void testCanMakeV1AndV2Requests_CloudLegacySolrClient()
      throws SolrServerException, IOException {
    try (final var v1RootClient =
        new CloudLegacySolrClient.Builder(List.of(zkHost), Optional.of(zkChroot)).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }
  }

  @Test
  public void testCanMakeV1AndV2Requests_CloudHttp2SolrClient()
      throws SolrServerException, IOException {
    try (final var v1RootClient =
        new CloudHttp2SolrClient.Builder(List.of(zkHost), Optional.of(zkChroot)).build()) {
      makeV1AndV2ListCollectionRequests(v1RootClient, COLL_NAME);
    }
  }

  /**
   * Validates that the provided client is able to make different varieties of v1 and v2 requests
   *
   * <p>Validation uses a number of SolrRequest objects to invoke essentially the same Solr
   * functionality, that of listing the known collections.
   *
   * @param client the SolrClient to use in all requests made to Solr.
   * @param expectedCollections the collections that Solr should know about.
   */
  @SuppressWarnings("unchecked")
  private void makeV1AndV2ListCollectionRequests(SolrClient client, String... expectedCollections)
      throws SolrServerException, IOException {
    // v1 SolrRequest, Traditional
    var actualCollList = CollectionAdminRequest.listCollections(client);
    assertThat(actualCollList, Matchers.containsInAnyOrder(expectedCollections));

    // v2 SolrRequest, generated by build
    final var v2ListReq = new CollectionsApi.ListCollections();
    final var v2ListRsp = v2ListReq.process(client).getParsed();
    assertThat(v2ListRsp.collections, Matchers.contains(expectedCollections));

    // V2Request
    final var v2ListReqAlt = new V2Request.Builder("/collections").GET().build();
    final var v2ListRspAlt = v2ListReqAlt.process(client);
    assertEquals(0, v2ListRspAlt.getStatus());
    actualCollList = (List<String>) v2ListRspAlt.getResponse().get("collections");
    assertThat(actualCollList, Matchers.contains(expectedCollections));

    // v1 SolrRequest, generic
    final var solrParams = new ModifiableSolrParams();
    solrParams.add("action", "list");
    final var v1ListReqGeneric =
        new GenericSolrRequest(SolrRequest.METHOD.GET, "/admin/collections", solrParams);
    final var v1ListRspGeneric = v1ListReqGeneric.process(client);
    actualCollList = (List<String>) v1ListRspGeneric.getResponse().get("collections");
    assertThat(actualCollList, Matchers.contains(expectedCollections));

    // v2 SolrRequest, generic
    final var v2ListReqGeneric = new GenericV2SolrRequest(SolrRequest.METHOD.GET, "/collections");
    final var v2ListRspGeneric = v2ListReqGeneric.process(client);
    actualCollList = (List<String>) v2ListRspGeneric.getResponse().get("collections");
    assertThat(actualCollList, Matchers.contains(expectedCollections));
  }
}
