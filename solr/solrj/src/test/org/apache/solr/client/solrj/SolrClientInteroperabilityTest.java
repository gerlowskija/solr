package org.apache.solr.client.solrj;

/**
 * Tests validating that all {@link SolrClient} implementations make both v1 and v2 requests without special setup.
 *
 * v1 and v2 APIs are very different from one another, even on the client side.  The tests in this class are meant to ensure that all SolrClient implementations are capable of sending both v1 and v2 requests successfully.  These tests are written partially in response to bugs like those described in the comments of SOLR-15735
 */
public class SolrClientInteroperabilityTest {
    // TODO 'extends SolrCloudTestCase'
}
