# Definition-Based API Mocking, Simulation, and Testing with [Microcks](https://microcks.io) - Demo Application

## Introduction

This [codecentric](https://codecentric.de) demo application illustrates the usage of Microcks for API mocking and
testing on the basis of API definitions expressed with [OpenAPI](https://github.com/OAI/OpenAPI-Specification). We rely
on [Testcontainers](https://testcontainers.com) to run the mock operations and execute tests against it leveraging
[REST-assured](https://rest-assured.io). This approach allows for consistent and automated integration testing both
local and in CI/CD pipelines.

In addition, this demo comprises a module that implements an [example API](#the-example-api) to the extent that it can
serve for Microcks-based contract testing.

## Illustrated Microcks Features

By means of the [example API](#the-example-api), we are able to showcase several of Microcks' core features:
- [Microcks Testcontainers module](https://testcontainers.com/modules/microcks)
- [Multi-artifacts](https://microcks.io/documentation/explanations/multi-artifacts)
- Dispatchers:
[Inferred URI PARTS dispatcher](https://microcks.io/documentation/explanations/dispatching/#inferred-dispatchers),
[JSON BODY dispatcher](https://microcks.io/documentation/explanations/dispatching/#json-body-dispatcher), and
[SCRIPT dispatcher](https://microcks.io/documentation/explanations/dispatching/#script-dispatcher) with
[context expressions](https://microcks.io/documentation/references/templates/#context-expression)
- [Dynamic mock content](https://microcks.io/documentation/explanations/dynamic-content) with
[function expressions](https://microcks.io/documentation/references/templates/#function-expressions)
- Programmatic usage of [Microcks' own API](https://microcks.io/documentation/references/apis/open-api) for mock
preparation beyond characteristics from API definitions
- [Interactive testing of contract conformance](https://microcks.io/documentation/explanations/conformance-testing)

## Running the Example for Microcks-Based Integration Testing

The example requires Maven and Java 21 or greater. Its implementation resides in the
[`integration-tests`](integration-tests) module and comprises a
[test suite](integration-tests/src/test/java/de/codecentric/microcks_demo/TestSuite.java) with a total of 14
REST-assured API tests which are executed against a Microcks testcontainer, whose preparation routine is implemented in
the
[`InfrastructureSetup` class](integration-tests/src/test/java/de/codecentric/microcks_demo/tests/infrastructure/InfrastructureSetup.java).

To spin up the testcontainer and execute the tests, run `mvn clean test` either from the directory in which the root
[`pom.xml`](pom.xml) file resides or, alternatively, from the module's sub-directory with the module's own
[`pom.xml`](integration-tests/pom.xml).

## Running the Example for Interactive Contract Testing with Microcks

In order to leverage Microcks and the prepared example API for interactive contract testing, first start the
[example API's](#the-example-api) demo implementation by issuing the terminal command `mvn spring-boot:run` from within
the directory of the [`api-implementation-app`](api-implementation-app) module.

Next, start the Microcks testcontainer from either the demo's root directory or the `integration-tests` sub-directory
with the terminal command `mvn clean test -Dtest="InfrastructureSetup"`. This command causes the application to pause
after container start and also prints the URI at which the UI of the Microcks server running inside the container is
reachable from a local browser.

With both the [example API's](#the-example-api) demo implementation and Microcks server running, it becomes possible to
leverage Microcks for the API's contract testing following these steps in the Microcks UI:

1. [Create a new conformance test](https://microcks.io/documentation/explanations/conformance-testing).
2. Specify `http://host.testcontainers.internal:${PORT}` as Test Endpoint with `${PORT}` being replaced by the port of
   the [example API's](#the-example-api) demo implementation that was previously started (`8081` by default).
   `http://host.testcontainers.internal` is
   [Testcontainers's base URI](https://java.testcontainers.org/features/networking) to enable access from a container
   (here: the Microcks server) to services running on the host (here: the [example API's](#the-example-api) demo
   implementation).
3. Choose `OPEN API SCHEMA` as test Runner.
4. Open the advanced options and Add Headers to the mocked API operations as follows (for details, see the
   [example API's description](#the-example-api)):  
   - `GET /customer_kinds`:  
      - Name: `api_key`
      - Value: `eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUElNb2NrIiwiaWF0IjoxNzIxMzY5MjQyLCJleHAiOjE3MjEzNjk1NDJ9.UqlkO_qW71spEEBZdJb6Oxe0j71U6_7Kdv6UotTbJUE`  
        (see the `api_key_token` property in the root [`pom.xml`](pom.xml)).
   - `GET /customer_kinds/{id}`:  
      - Name: `api_key`
      - Value: `eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUElNb2NrIiwiaWF0IjoxNzIxMzY5MjQyLCJleHAiOjE3MjEzNjk1NDJ9.UqlkO_qW71spEEBZdJb6Oxe0j71U6_7Kdv6UotTbJUE`  
        (see the `api_key_token` property in the root [`pom.xml`](pom.xml)). 
   - `POST /login`:  
      - Name: `api_key`
      - Value: `eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUElNb2NrIiwiaWF0IjoxNzIxMzY5MjQyLCJleHAiOjE3MjEzNjk1NDJ9.UqlkO_qW71spEEBZdJb6Oxe0j71U6_7Kdv6UotTbJUE`  
        (see the `api_key_token` property in the root [`pom.xml`](pom.xml)).
   - `GET /customer`:  
      - Name: `customer_token`
      - Value: `eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUElNb2NrIiwiZW1haWwiOiJzb21lX2N1c3RvbWVyQGV4YW1wbGUub3JnIn0.7TECmPgKMLftw2kK_CncM_AK0r7HAY7AmkV0y7qSA5Y`  
        (see the `some_customer_token` property in the root [`pom.xml`](pom.xml)).   
5. Launch the test and observe that the API fulfills its expected contract with a score of ca. 66%. That is because the 
   `ìnvalid_credentials` tests for the `POST /login` and `GET /customer` operations fail. The rationales of these test
   failures are (i) `POST /login` currently returning HTTP `400 BAD REQUEST` instead of `401 UNAUTHORIZED` for empty
   request bodies; and (ii) `GET /customer` returning HTTP `200 OK` instead of `401 UNAUTHORIZED` as Microcks currently
   only allows for specifying headers for tested operations as a whole and not operation-specific test cases (see the
   [example API's description](#the-example-api) for details about header semantics of `GET /customer`).
   
## The Example API

The demo application comprises an [OpenAPI definition for an example API](api-spec/customers.yaml) that is loaded into
started Microcks testcontainers and accompanied by examples from a
[dedicated Postman collection](api-spec/customers_examples.postman_collection.json).

**Disclaimer:** We discovered this API in the wild, and found it quite coherent, understandable, and therefore suitable
to illustrate some of Microcks' core features. However, it also bears some potential for optimization and does not
reflect a style of API design
[that we would advise](https://www.codecentric.de/leistungen/api-experience-api-operations).

The example API is rooted in the domain of Customer Relationship Management and provides the following operations:
 
- `GET /customer_kinds`  
Retrieve information about the supported kinds of customers. For its execution, this operation expects a fixed API key
in the form of a [JSON Web Token (JWT)](https://jwt.io) to be provided upon calling the operation as value for the HTTP
- header field `api_key`. An example response to a valid operation call looks as follows:
```json
{
  "1": "Private person",
  "2": "Commercial enterprise",
  "3": "Public institution"
}
```

- `GET /customer_kinds/{id}`  
Retrieve information about a certain supported kind of customer. For this purpose, the operation expects the same API
key token as `GET /customer_kinds` in the HTTP header field `api_key`. Invoking the operation for the customer kind with
ID `3` could yield the following response:
```json
{
  "id": "3",
  "kind": "Public institution",
  "createdOn": "12.07.2024 12:32:59",
  "updatedOn": ""
}
```

- `POST /login`  
Log in a customer or other user. This operation requires the API key token in the same fashion as `GET /customer_kinds`
and `GET /customer_kinds/{id}` and returns a user-specific JWT for use with operations that implement user-specific
behavior, e.g., `GET /customer` (see below). The following listing shows a valid example request to the operation:
```json
{
  "email": "some_customer@example.org",
  "password": "123456"
}
```
This request results in a response like
```json
{
  "customer_token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJBUElNb2NrIiwiZW1haWwiOiJzb21lX2N1c3RvbWVyQGV4YW1wbGUub3JnIn0.7TECmPgKMLftw2kK_CncM_AK0r7HAY7AmkV0y7qSA5Y"
}
```

whereas invalid credentials would result in an HTTP response with status code `401 UNAUTHORIZED` and an empty body.

- `GET /customer`  
This operation allows for the retrieval of a customer's details including first name, last name, and address. The
customer is identified by submitting their specific login token (see the description of the `POST /login` operation) to
the operation as a value for the HTTP header field `customer_token`. Assuming that a valid login token was submitted,
the operation results in an HTTP response like 
```json
{
  "email": "some_customer@example.org",
  "first_name": "Casey",
  "last_name": "Rice",
  "company_name": "",
  "address": "6377 Fallon Pine, North Emoryburgh, Anguilla"
}
```

whereas an invalid login token would lead to an HTTP response with status code `401 UNAUTHORIZED` and an empty body.