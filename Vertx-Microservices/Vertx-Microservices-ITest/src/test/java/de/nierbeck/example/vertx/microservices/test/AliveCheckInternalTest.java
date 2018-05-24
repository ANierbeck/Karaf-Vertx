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
package de.nierbeck.example.vertx.microservices.test;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AliveCheckInternalTest {

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected static Vertx vertxService;
    
    @Inject
    protected EventBus eventBus;

    /**
     * To make sure the tests run only when the boot features are fully
     * installed
     */
    @Inject
    BootFinished bootFinished;

    @Configuration
    public Option[] configuration() {
        return new Option[] { karafDistributionConfiguration()
                .frameworkUrl(
                            maven()
                                .groupId("de.nierbeck.example.vertx.mircoservices")
                                .artifactId("Vertx-Microservices-Karaf")
                                .type("tar.gz")
                                .versionAsInProject())
                .unpackDirectory(new File("target/paxexam/unpack/"))
                .useDeployFolder(false)
                .runEmbedded(false) //only for debugging
                , features(
                        maven().groupId("de.nierbeck.example.vertx.mircoservices")
                            .artifactId("Vertx-Microservices-Features")
                            .type("xml")
                            .classifier("features").versionAsInProject(),
                        "Vertx-AliveCheck")
                , configureConsole().ignoreLocalConsole(), logLevel(LogLevel.INFO), keepRuntimeFolder()
                ,  mavenBundle().groupId("io.vertx").artifactId("vertx-unit").versionAsInProject()
            };
    }


    @Test
    public void subRunner() {
        System.out.println("Calling sub-runner!!");
        Result result = JUnitCore.runClasses(SubTestWithRunner.class);
//        assertThat(result.wasSuccessful(), is(true));
        if(!result.wasSuccessful()) {
            result.getFailures().stream().forEach(failure -> fail(failure.getMessage()));
        }
    }
    

    @Test
    public void checkAliveCheckFeature() throws Exception {
        assertThat(featuresService.isInstalled(featuresService.getFeature("Vertx-AliveCheck")), is(true));
    }
    
    @Test 
    public void checkVertxNotNull() {
        assertThat(vertxService, notNullValue());
    }
    
    @RunWith(VertxUnitRunner.class)
    public static class SubTestWithRunner {
        
        @BeforeClass
        public static void init() {
            System.out.println("Before test");
        }
        
        @Test
        public void checkAliveCheckNavigable(TestContext context) {
            System.out.println("testing");
            Async async = context.async();
            System.out.println("vertxService: "+vertxService);
            vertxService.createHttpClient().getNow(8080, "localhost", "/health/ping", response -> {
                System.out.println("response: "+response);
                System.out.println("Status-Code: "+response.statusCode());
                context.assertEquals(response.statusCode(), 200);
                System.out.println("status code: "+response.statusCode());
                context.assertEquals(response.headers().get("content-type"), "application/json;charset=UTF-8");
                response.bodyHandler(body -> {
                    context.assertTrue(body.toString().contains("{\"checks\":[{\"id\":\"ping-handler\",\"status\":\"UP\"}],\"outcome\":\"UP\"}"));
                    async.complete();
                });
            });
        }
    }
    
}
