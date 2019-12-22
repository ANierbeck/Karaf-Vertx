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

import java.io.File;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CustomVertxKarafTest extends KarafTestSupport {

    @Inject
    private BundleContext bc;

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected Vertx vertxService;

    /**
     * To make sure the tests run only when the boot features are fully
     * installed
     */
    @Inject
    BootFinished bootFinished;

    @Configuration
    public Option[] config() {
        return new Option[] {
                karafDistributionConfiguration()
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
                KarafDistributionOption.replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
                logLevel(LogLevel.INFO),
                keepRuntimeFolder(),
                KarafDistributionOption.configureSecurity().disableKarafMBeanServerBuilder(),
                CoreOptions.mavenBundle().groupId("org.awaitility").artifactId("awaitility").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),
                new VMOption("--add-reads=java.xml=java.logging"),
                new VMOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                new VMOption("--patch-module"),
                new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + System.getProperty("karaf.version") + ".jar"),
                new VMOption("--patch-module"),
                new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + System.getProperty("karaf.version") + ".jar"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.security=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.net=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.lang=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.util=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                new VMOption("-classpath"),
                new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
            };
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

}
