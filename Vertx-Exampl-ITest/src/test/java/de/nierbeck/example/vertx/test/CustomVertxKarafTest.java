/*
   Copyright 2016 Achim Nierbeck

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package de.nierbeck.example.vertx.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CustomVertxKarafTest {

    @Inject
    private BundleContext bc;

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected Vertx vertxService;

    @Inject
    protected SessionFactory sessionFactory;
    

    private ExecutorService executor = Executors.newCachedThreadPool();

    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private PrintStream printStream = new PrintStream(byteArrayOutputStream);
    private PrintStream errStream = new PrintStream(byteArrayOutputStream);
    private Session session;

    /**
     * To make sure the tests run only when the boot features are fully
     * installed
     */
    @Inject
    BootFinished bootFinished;

    @Configuration
    public static Option[] configuration() throws Exception {
        return new Option[] { karafDistributionConfiguration()
                .frameworkUrl(
                            maven()
                                .groupId("de.nierbeck.example.vertx")
                                .artifactId("Vertx-Karaf")
                                .type("tar.gz")
                                .versionAsInProject())
                .unpackDirectory(new File("target/paxexam/unpack/"))
                .useDeployFolder(false)
                .runEmbedded(false), //only for debugging 
                configureConsole().ignoreLocalConsole(), 
                logLevel(LogLevel.INFO), keepRuntimeFolder()
            };
    }
    
    @Before
    public void setUpITest() throws Exception {
        session = sessionFactory.create(System.in, printStream, errStream);
    }

    @After
    public void cleanupITest() throws Exception {
        session = null;
    }
    
    @Test
    public void shouldHaveBundleContext() {
        assertThat(bc, is(notNullValue()));
    }

    @Test
    public void checkVertxFeature() throws Exception {
        assertThat(featuresService.isInstalled(featuresService.getFeature("Vertx-Feature")), is(true));
    }

    @Test
    public void checkVertexService() {
        assertThat(vertxService, is(notNullValue()));
    }

    @Test
    public void installVerticle() throws Exception {

        assertThat(vertxService, is(notNullValue()));

        MyVerticle verticle = new MyVerticle();

        bc.registerService(Verticle.class, verticle, null);

        // make sure it's started
        Thread.sleep(2000);

        assertThat(verticle.isStarted(), is(true));
    }
    
    @Test
    public void testVerticlesList() throws Exception {
        assertThat(executeCommand("verticles:list"), containsString("MyVerticle"));
    }
    
    public class MyVerticle extends AbstractVerticle {
        private boolean started = false;

        public boolean isStarted() {
            return started;
        }

        @Override
        public void start() throws Exception {
            started = true;
        }
    }
    
    protected String executeCommand(final String command) throws IOException {
        byteArrayOutputStream.flush();
        byteArrayOutputStream.reset();

        String response;
        FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
            public String call() {
                try {
                    System.err.println(command);
                    session.execute(command);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                printStream.flush();
                errStream.flush();
                return byteArrayOutputStream.toString();
            }
        });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(10000L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }

        System.err.println(response);

        return response;
    }
}
