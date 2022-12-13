package org.apache.solr.bench.index;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.IOUtils;
import org.apache.solr.embedded.JettyConfig;
import org.apache.solr.embedded.JettySolrRunner;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.apache.solr.bench.BaseBenchState.log;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(time = 30, iterations = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
@Fork(value = 1)
public class SolrStartup {

    @Benchmark
    public void startSolr(PerThreadState threadState) throws Exception {
        log("In startSolr benchmark");
        threadState.solrRunner.start(false); // 'false' tells Jetty to not go out of its way to reuse any previous ports
    }

    @State(Scope.Thread)
    public static class PerThreadState {

        public File tmpSolrHome;
        public JettySolrRunner solrRunner;

        @Setup(Level.Trial)
        public void bootstrapJettyServer() throws Exception {
            log("In trial-level setup 'bootstrapJettyServer'");
            tmpSolrHome = Files.createTempDirectory("solrstartup-perthreadstate-jsr").toFile().getAbsoluteFile();
            FileUtils.copyDirectory(
                    new File("/Users/gerlowskija/checkouts/solr/solr/core/src/test-files/solr", "configsets/minimal/conf"), new File(tmpSolrHome, "/conf"));
            FileUtils.copyFile(new File("/Users/gerlowskija/checkouts/solr/solr/core/src/test-files/solr", "solr.xml"), new File(tmpSolrHome, "solr.xml"));

            solrRunner =
                    new JettySolrRunner(tmpSolrHome.getAbsolutePath(), buildJettyConfig("/solr"));
        }

        // Ensure that JettySolrRunner is always stopped - only strictly necessary _after_ a Benchmark, but coded
        // defensively in case of changes.
        //@Setup(Level.Iteration)
        @TearDown(Level.Iteration)
        public void stopJettyServerIfNecessary() throws Exception {
            // Alright, This is the problem here - this method is only getting invoked at the end of 50 or so
            // invocations of the benchmark method.  (See the output from running `./jmh.sh SolrStartup 2>&1 | tee output.txt`)
            //
            // I was expecting it would be run after call to the benchmark method. Since I'm only closing 1 out of 50 or so
            // Jetty servers that I start, we orphan just tons and tons of file handles.
            //
            // I _think_ this is because since this file uses BenchmarkMode=THROUGHPUT, an 'iteration' is defined by a
            // certain amount of elapsed time, and JMH stuffs as many benchmark-method calls into that time window as possible.
            //
            // Assuming that's right, I might be able to fix this by changing this method to use
            // @TearDown(Level.Invocation), but the Javadocs warn heavily against that option. Maybe those caveats are
            // less relevant to this not-very-micro benchmarking use-case.  Another option would be to change the
            // BenchmarkMode to SingleShotTime and cranking up the iterations; I think that would work?
            log("In stopJettyServerifNecessary");
            //if (solrRunner.isRunning()) {
                solrRunner.stop();
            //}
        }

        @TearDown(Level.Trial)
        public void destroyJettyServer() throws Exception {
            log("In trial-level cleanup 'destroyJettyServer'");
            if (solrRunner.isRunning()) {
                solrRunner.stop();
            }

            IOUtils.rm(Path.of(tmpSolrHome.toURI()));
        }
        private static JettyConfig buildJettyConfig(String context) {
            return JettyConfig.builder()
                    .setContext(context)
                    .stopAtShutdown(true)
                    .build();
        }
    }


}
