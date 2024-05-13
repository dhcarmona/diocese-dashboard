# Diocese Dashboard project

## Description

This simple Spring Boot project will serve as a replacement for the current Google Forms based approach for church reports and statistics for the Episcopal Church in Costa Rica.

## Building

It's a regular Maven/Spring Boot project. To build and generate the JAR file, run
  mvn clean package

## Generating the schema

There's a separate main class in charge of generating the schema. Run
  mvn compile exec:java

To run the code that generates the schema. The SQL will be generated in a schema.sql file in the root folder.
