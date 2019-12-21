# How to combine Vert.x with Karaf

This project was created to show how easy it is to create new Verticles as OSGi services and have a dynamic runtime 
which takes care of those verticles automatically. 

The project consists of two different parts.
The underlying infrastructure needed to automatically detect Verticles as services and to bind them during run-time. 
The second part is a sample infrastructure showing how to create "microservices" with Vertx and Karaf as run-time. 

## Infrastructure

The underlying infrastructure consists of a minimal Apache Karaf server, which is adapted with the infrastructure bundles. 
This customized Apache Karaf contains the following extras. 

### Vertx-System

Consists of the Vertx service tracker, which will track all Verticle services and registers these inside the Vertx system. 
 
### Vertx-Extender

The extender bundle takes care of most of the stuff needed to run verticles as OSGi services. 
For this the extender bundle checks all other bundles which are installed, if they are capable of importing the Verticle class. 
This is the first indicator that it's actually a relevant bundle. Now the extender checks if any of the classes inside 
the bundle is a Verticle implementation. If so, this Verticle class is instantiated and registered as OSGi Service. 

### Vertx-Shell

Customized Apache Karaf Shell commands for easier interaction with Verticles inside Apache Karaf. 
The following list of commands is available with this bundle: 
- verticles:list - will list all verticles registered as services
- vertx:netlist - lists all running vertx servers
- vertx:metrics - gives an overview of metrics around vertx
- vertx:local-map-put - puts a key value to a local map
- vertx:local-map-get - gets a value depending on a given key
- vertx:local-map-rm  - removes a value and key, depending on the given key
- vertx:bus-send - sends a message via the event bus
- vertx:bus-tail - tails the messages sent via the event bus

### Vertx-Http

Starts a specialized verticle, as Http Server. 

### Vertx-Cluster

enables cluster ability via Hazelcast

### Vertx-AliveCheck

special verticle which is registered as http service to check if the instance is available.

##  Vertx based Microservices

The Vertx Microservices sub-module, consists of a simple Example application. 