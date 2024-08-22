package org.apache.solr.handler.api;

import org.apache.solr.SolrTestCase;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ExecutorUtil;
import org.apache.solr.common.util.SolrNamedThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ManyShardPerfTest extends SolrTestCase {

    private static final String[] SOLR_URLS = new String[] {"http://localhost:8983/solr", "http://localhost:8984/solr", "http://localhost:7574/solr", "http://localhost:7575/solr"};
    private static final int NUM_THREADS = 3 * SOLR_URLS.length;
    private static final int QUERIES_PER_THREAD = 100;
    private static final String QUERY_FIELD = "subject_clean_txt";
    private static final String[]  QUERY_TERMS = new String[] {"solr", "release", "for", "vote", "to", "discuss", "in", "the", "and", "new", "on", "rc1", "branch", "community", "node", "of", "lucene", "virtual", "welcome", "9.0.0", "class", "meetup", "roles", "support", "8", "first", "build", "operator", "as", "feature", "freeze", "committer", "2023", "apache", "with", "utf", "main", "proposal", "a", "re", "bugfix", "apis", "code", "rc2", "pmc", "from", "api", "is", "jenkins", "v2", "b", "tests", "9.0", "guide", "sip", "version", "solrcloud", "be", "should", "jdk", "use", "q", "2024", "changes", "using", "issue", "java", "query", "module", "search", "9.2.1", "64bit", "8.11.3", "default", "ref", "docker", "linux", "mode", "released", "9.5.0", "failing", "not", "result", "an", "announce", "jax", "jira", "pr", "rs", "9.1.0", "dependency", "or", "snapshot", "unstable", "bin", "how", "rc3", "reference", "request", "solr's", "while", "8.11", "9.7", "deprecate", "errors", "more", "up", "we", "x", "running", "all", "data", "possible", "slow", "cleaning", "collection", "intellij", "single", "solrj", "april", "experimental", "v0.3.0", "9.1.1", "base", "pin", "test", "why", "5bjenkins", "9.1", "receiving", "5d_solr_", "bb_solr", "chair", "may", "next", "1gigabit", "3a_", "buffer", "etc", "fastwriter", "low", "networks", "output", "reason", "scroll", "threaded", "untunable", "warnings", "8.10.0", "error", "response", "21", "still", "update", "created", "gradle", "help", "naming", "steps", "9.4.1", "admin", "c2", "leader", "tracking", "via", "8.10", "9", "shard", "soon", "updated", "zkcmdexecutor", "9.3.0", "9.4", "at", "backup", "ci", "has", "jetty", "needed", "our", "replica", "some", "state", "ui", "working", "broader", "github", "gpg", "hotspot", "8.11.2", "can", "change", "down", "image", "storage", "builds", "command", "2022", "after", "alone"};
    private static final String MANY_SHARD_COLLECTION = "gettingstarted";

    private static SolrClient[] SOLR_CLIENTS;

    @Before
    public void setupClients() {
        SOLR_CLIENTS = createClientsFromUrls(SOLR_URLS);
    }

    @After
    public void teardownClients() throws IOException {
        for (SolrClient client : SOLR_CLIENTS) {
            client.close();
        }
    }

    @Test
    public void manyShardPerformanceTest() throws Exception {
        final var executorService = ExecutorUtil.newMDCAwareFixedThreadPool(NUM_THREADS, new SolrNamedThreadFactory("query-runner"));
        final var taskFutures = new ArrayList<Future<Long>>();

        // Kick off the QueryRunner's
        for (int i = 0 ; i < NUM_THREADS ; i++) {
            taskFutures.add(executorService.submit(new QueryRunner()));
        }

        long totalQTime = 0L;
        for (Future<Long> future : taskFutures) {
            final long totalThreadQTime = future.get();
            final long avgThreadQTime = totalThreadQTime / QUERIES_PER_THREAD;
            totalQTime += totalThreadQTime;
            System.out.println("Thread finished with totalQTime=" + totalThreadQTime + ", averageQTime=" + avgThreadQTime);
        }

        final long avgQTime = totalQTime / (QUERIES_PER_THREAD * taskFutures.size());
        System.out.println("Overall execution finished with totalQTime=" + totalQTime + ", averageQTime=" + avgQTime);
    }

    private class QueryRunner implements Callable<Long> {
        public Long call() {
            long threadQTime = 0;
            for (int i = 0 ; i < QUERIES_PER_THREAD ; i++) {
                final var client = randomSolrClient();
                final var req = randomQueryRequest();
                final var rsp = sendRequest(client, req);
                if (rsp.getStatus() != 0) {
                    throw new RuntimeException("Unexpected status [" + rsp.getStatus() + "] for query [" + req.getParams().get("q") + "].  Response is: " + rsp);
                }
                threadQTime += rsp.getQTime();
            }

            return threadQTime;
        }
    }

    private QueryResponse sendRequest(SolrClient client, QueryRequest req) {
        try {
            return req.process(client, MANY_SHARD_COLLECTION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SolrClient[] createClientsFromUrls(String[] solrUrls) {
        final var clientArr = new SolrClient[solrUrls.length];
        for (int i = 0 ; i < solrUrls.length ; i++) {
            clientArr[i] = new Http2SolrClient.Builder(solrUrls[i]).build();
        }

        return clientArr;
    }

    private SolrClient randomSolrClient() {
        return SOLR_CLIENTS[random().nextInt(SOLR_CLIENTS.length)];
    }

    private QueryRequest randomQueryRequest() {
        final var queryParams = new ModifiableSolrParams();
        queryParams.add("q", randomQueryStr());
        queryParams.add("rows", "10");
        queryParams.add("start", String.valueOf(random().nextInt(10))); // Adds a little extra randomization to requests
        queryParams.add("cache", "false");
        return new QueryRequest(queryParams);
    }

    private String randomQueryStr() {
        final String queryTerm = QUERY_TERMS[random().nextInt(QUERY_TERMS.length)];
        return QUERY_FIELD + ":" + queryTerm;
    }
}
