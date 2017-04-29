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
        RestAssured.port = Integer.getInteger("http.port", 8080);
    }
    
    @After
    public void cleanupITest() throws Exception {
        RestAssured.reset();
    }

    @Test
    public void checkBooksAvailable() throws Exception {
        final int id = get("/cookbook-service/").then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath().getInt("find { it.name=='Java Cookbook' }.id");
        
        get("/cookbook-service/" + id).then()
        .assertThat()
        .statusCode(200)
        .body("name", equalTo("Java Cookbook"))
        .body("isbn", equalTo("1234-56789"))
        .body("id", equalTo(id));
    }
    
    @Test
    public void checkRecipesForBook() throws Exception {
        final int id = get("/cookbook-service/1/recipe").then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath().getInt("find { it.name=='Singletons' }.id");
        
        get("/cookbook-service/1/recipe/"+ id).then()
        .assertThat()
        .statusCode(200)
        .body("name", equalTo("Singletons"));
    }
    
}
