# Vertex example project running on Apache Karaf

[![Build Status](https://travis-ci.org/ANierbeck/Karaf-Vertx.svg)](https://travis-ci.org/ANierbeck/Karaf-Vertx)

This is a simple sample showing how to run Vertx applications on Apache Karaf. 

This includes:
- Vertx and Bus as service   
- A customized shell to show:
  - The registered verticles  
  - What communications is beeing held on the bus
  - Send data to the bus
  - Put/Get/Remove data from the local map
  - List the Http Server registered
  - Show metrics
- A WhiteBoard Extender to register verticles as services   
- A Karaf Feature File   
- A Karaf custom Distribution for easy testing   
- A submodule as example on how to use this, for "microservices" with vertx
  - Extra Samples
  - Extra feature file to completly install those samples

KUDOS to https://github.com/vert-x3/vertx-examples/tree/master/osgi-examples for inspiring me to work on this. 
