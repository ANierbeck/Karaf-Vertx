package de.nierbeck.example.vertx.microservices.test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.ops4j.pax.exam.junit.PaxExamServer;

import io.restassured.RestAssured;

public class MicroservicesExternalTest {

    @ClassRule
    public static PaxExamServer exam = new PaxExamServer(MicroservicesTest.class);
    
    @Before
    public void setUpITest() throws Exception {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8000);
    }
    
    @After
    public void cleanupITest() throws Exception {
        RestAssured.reset();
    }

    @Test
    public void checkBooksAvailable() throws Exception {
        final int id = get("/cookbook/").then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath().getInt("find { it.name=='Java Cookbook' }.id");
        
        get("/cookbook/" + id).then()
        .assertThat()
        .statusCode(200)
        .body("name", equalTo("Java Cookbook"))
        .body("isbn", equalTo("1234-56789"))
        .body("id", equalTo(id));
    }
    
    @Test
    public void checkRecipesForBook() throws Exception {
        final int id = get("/cookbook/1/recipe").then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath().getInt("find { it.name=='Singletons' }.id");
        
        get("/cookbook/1/recipe/"+ id).then()
        .assertThat()
        .statusCode(200)
        .body("name", equalTo("Singletons"));
    }
    
}
