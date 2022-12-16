package org.apache.solr.bench;

import org.apache.commons.io.FileUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.embedded.JettySolrRunner;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.apache.solr.bench.BaseBenchState.log;

public class AsdfTest extends SolrTestCaseJ4 {

    private static final int NUM_CORES = 80;
    private File tmpSolrHome;

    private JettySolrRunner solrRunner;


    @Test
    public void asdf() throws Exception {
        log("In trial-level setup 'bootstrapJettyServer'");
        tmpSolrHome = Files.createTempDirectory("solrstartup-perthreadstate-jsr").toFile().getAbsoluteFile();
        final File configsetsDir = new File(tmpSolrHome, "configsets");
        Files.createDirectory(configsetsDir.toPath());
        FileUtils.copyDirectory(
                new File("/Users/gerlowskija/checkouts/solr/solr/core/src/test-files/solr", "configsets/minimal/conf"), new File(configsetsDir, "/defaultConfigSet/conf"));
        FileUtils.copyFile(new File("/Users/gerlowskija/checkouts/solr/solr/core/src/test-files/solr", "solr.xml"), new File(tmpSolrHome, "solr.xml"));

        solrRunner =
                new JettySolrRunner(tmpSolrHome.getAbsolutePath(), buildJettyConfig("/solr"));
        solrRunner.start(false);
        try (SolrClient client = solrRunner.newClient()) {
            for (int i = 0; i < NUM_CORES; i++) {
                createCore(client, "core-prefix-" + i);
            }
        }
        solrRunner.stop();
    }

    private void createCore(SolrClient client, String coreName) throws Exception {
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(coreName);
        create.setConfigSet("defaultConfigSet");

        final CoreAdminResponse response = create.process(client);
        if (response.getStatus() != 0) {
            throw new RuntimeException("Some error creating core: " + response.jsonStr());
        }
    }
}
