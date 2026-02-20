# Diocese Dashboard project

## Description

This simple Spring Boot project will serve as a replacement for the current Google Forms based approach for church reports and statistics for the Episcopal Church in Costa Rica.

## Project Goals

The application targets two types of users:

### Admin Users
- Log in to the system to manage core data.
- Add and remove **Churches** and **Celebrants**.
- Define **Service Info Items** — the fields that make up a service report (e.g., attendance count, offering amount), each with a specific type.
- Create **Service Templates**, which bundle a set of Service Info Items together with a required Church field and an optional Celebrant field.
- Generate a **unique URL** for each Service Template to share with regular users.
- Can also do everything a Regular User can do.

### Regular Users
- Access a Service Template via its unique URL.
- Create **Service Instances** by:
  - Selecting the Church the service belongs to.
  - (Optionally) selecting the Celebrant for that service.
  - Filling in the Service Info Items defined by the template.
- Each Service Instance is tied to a specific date, and users may submit multiple instances for the same template.

---

## Building

It's a regular Maven/Spring Boot project. To build and generate the JAR file, run
  mvn clean package

## Generating the schema

There's a separate main class in charge of generating the schema. Run
  mvn compile exec:java

To run the code that generates the schema. The SQL will be generated in a schema.sql file in the root folder.
