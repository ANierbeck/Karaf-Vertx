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

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class VertxExtenderKarafTest {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
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
    // @formatter:off
    public Option[] configuration() {
        return new Option[] {
                karafDistributionConfiguration()
                        .frameworkUrl(
                                maven()
                                    .groupId("de.nierbeck.example.vertx")
                                    .artifactId("Vertx-Karaf")
                                    .type("tar.gz").versionAsInProject()
                                )
                        .unpackDirectory(new File("target/paxexam/unpack/"))
                        .useDeployFolder(false)
                        .runEmbedded(false), // only for debugging
                configureConsole().ignoreLocalConsole(),
                KarafDistributionOption.replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),

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

                logLevel(LogLevel.DEBUG), 
                keepRuntimeFolder()
       };
    }
    // @formatter:on

    @Test
    public void installExtendedBundle() throws Exception {
        logger.info("testing extender ... ");
        String bundlePath = "mvn:de.nierbeck.example.vertx/Vertx-Extended-Verticles/0.3.0-SNAPSHOT";
        logger.info("installing bundle with url: {}", bundlePath);
        Bundle bundle = bc.installBundle(bundlePath);
        bundle.start();
        int count = 0;
        while (bundle.getState() != Bundle.ACTIVE && count < 50) {
            Thread.sleep(1000);
            count = count++;
        }
        
        assertTrue (bundle.getState() == Bundle.ACTIVE);
        logger.info("Bundle is active");
        
        Thread.sleep(1000); //wait a second so the service is registered. 

        Collection<ServiceReference<Verticle>> serviceReferences = bc.getServiceReferences(Verticle.class, null);
        
        logger.info("found {} services", serviceReferences.size());
        List<ServiceReference<Verticle>> collect = serviceReferences.stream().filter(serviceReference -> serviceReference.getBundle() == bundle).collect(Collectors.toList());
        assertTrue(collect.size() > 0);
    }

    public File getConfigFile(String path) {
        URL res = this.getClass().getResource(path);
        if (res == null) {
            throw new RuntimeException("Config resource " + path + " not found");
        }
        return new File(res.getFile());
    }
}
