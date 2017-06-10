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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Assert;
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
import org.osgi.framework.ServiceReference;

import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import de.nierbeck.example.vertx.http.Route;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MicroservicesTest {

    @Inject
    private BundleContext bc;

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected Vertx vertxService;
    
    @Inject
    protected EventBus eventBus;
    
    @Inject
    protected DataSource dataSource;

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

    private Connection connection;

    @Configuration
    public Option[] configuration() throws Exception {
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
                , configureConsole().ignoreLocalConsole(), logLevel(LogLevel.INFO), keepRuntimeFolder() 
            };
    }
    
    @Before
    public void setUpITest() throws Exception {
        session = sessionFactory.create(System.in, printStream, errStream);
        
        connection = dataSource.getConnection();
    }

    @After
    public void cleanupITest() throws Exception {
        connection.close();
        
        connection = null;
        session = null;
    }
    
    @Test
    public void shouldHaveBundleContext() {
        assertThat(bc, is(notNullValue()));
    }

    @Test
    public void checkEventFeature() throws Exception {
        assertThat(featuresService.isInstalled(featuresService.getFeature("Vertx-Feature")), is(true));
    }

    @Test
    public void checkVertexService() {
        assertThat(vertxService, is(notNullValue()));
    }
    
    @Test
    public void testServerList() throws Exception {
        awaitRouteAndHttpService();
        assertThat(executeCommand("vertx:netlist"), CoreMatchers.containsString("8080"));
    }
    
    @Test
    public void checkMicroserviceVerticlesAvailable() throws Exception {
        assertThat(executeCommand("verticles:list"), CoreMatchers.containsString("JdbcServiceVertcl"));
        assertThat(executeCommand("verticles:list"), CoreMatchers.containsString("CookBookServiceVertcl"));
        assertThat(executeCommand("verticles:list"), CoreMatchers.containsString("VertxHttpServer"));
    }

    @Test
    public void sendInsertPerBus() throws Exception {
        
        writeBookToBus();
        
        Thread.sleep(1000); //used in the test, to make sure the async call is executed!
        
        Statement statement = connection.createStatement();
        ResultSet query = statement.executeQuery("SELECT * FROM BOOK;");
        assertTrue(query.next());
        assertTrue(query.next());
        assertThat(query.getLong(1), is(2l));
        statement.close();
        
        writeRecipeToBus();

        Thread.sleep(1000); //used in the test, to make sure the async call is executed!
        
        statement = connection.createStatement();
        query = statement.executeQuery("SELECT * FROM RECIPE WHERE book_id = 2;");
        
        assertTrue(query.next());
        assertThat(query.getLong(1), is(2l));
        assertThat(query.getLong(4), is(2l));
        assertThat(query.getString(2), containsString("testRecipe"));
        
        //cleanup
        statement = connection.createStatement();
        statement.execute("DELETE FROM RECIPE WHERE book_id = 2;");
        statement.execute("DELETE FROM BOOK WHERE id = 2;");
        
    }
    
    @Test
    public void sendUpdatePerBus() throws Exception {
        Statement statement = connection.createStatement();
        
        Recipe recipe = new Recipe(2l,"testRecipe", "testIngredient", 1l);
        eventBus.send("de.nierbeck.vertx.jdbc.write.add", recipe);
        
        Thread.sleep(1000); //used in the test, to make sure the async call is executed!
        
        recipe.setIngredients("testIngredientUpdated");
        
        eventBus.send("de.nierbeck.vertx.jdbc.write.update", recipe);
        
        Thread.sleep(1000); //used in the test, to make sure the async call is executed!
        
        statement = connection.createStatement();
        ResultSet query = statement.executeQuery("SELECT * FROM RECIPE WHERE book_id = 1;");

        assertTrue(query.next());
        assertTrue(query.next());
        assertThat(query.getLong(1), is(2l));
        assertThat(query.getLong(4), is(1l));
        assertThat(query.getString(3), containsString("testIngredientUpdated"));
        
        //Cleanup
        statement.execute("DELETE FROM RECIPE WHERE id = 2");
    }
    
    @Test
    public void readDataPerBus() throws Exception {
        
        eventBus.send("de.nierbeck.vertx.jdbc.read", new Book(1l, null, null), message -> {
            assertThat(message.result().body(), IsInstanceOf.instanceOf(Book.class));
            Book book = (Book) message.result().body();
            assertThat(book.getId(), is(1l));
            assertThat(book.getIsbn(), containsString("1234-56789"));
            assertThat(book.getName(), containsString("Java Cookbook"));
        });
    }

    @Test
    public void readBooksFromBus() throws Exception {
        eventBus.send("de.nierbeck.vertx.jdbc.read", new Book(), message -> { 
            assertFalse(message.failed());
            assertThat(message.result().body(), IsInstanceOf.instanceOf(List.class));
            
            List<Book> listOfBooks = (List<Book>)message.result().body();
            assertThat(listOfBooks.size(), is(1));

            assertThat(listOfBooks.get(0), IsInstanceOf.instanceOf(Book.class));
            assertThat(listOfBooks.get(0).getId(), is(1l));
            assertThat(listOfBooks.get(0).getIsbn(), containsString("1234-56789"));
            assertThat(listOfBooks.get(0).getName(), containsString("Java Cookbook"));
        });
    }
    
    private void writeRecipeToBus() {
        Recipe recipe = new Recipe(2l,"testRecipe", "testIngredient", 2l);
        eventBus.send("de.nierbeck.vertx.jdbc.write.add", recipe);
    }

    private void writeBookToBus() {
        Book book = new Book(2l, "testBook", "testISBN");
        eventBus.send("de.nierbeck.vertx.jdbc.write.add", book);
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
    

    private void awaitRouteAndHttpService() throws Exception {
        Collection<ServiceReference<Route>> serviceReferences = bc.getServiceReferences(Route.class, null);
        int count = 0;
        while (serviceReferences == null && serviceReferences.size() < 2 && count < 10) {
            Thread.sleep(200);
            serviceReferences = bc.getServiceReferences(Route.class, null);
            count++;
        }
        
        Assert.assertNotNull("Failed to get Route service", serviceReferences);
    }

}
