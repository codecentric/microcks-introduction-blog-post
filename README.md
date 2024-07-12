# [Microcks](https://microcks.io) Demo for [Testcontainers](https://testcontainers.com)-Based API Mocking and Testing

## Introduction

This [codecentric](https://codecentric.de) demo application illustrates the application of Microcks for API mocking and
testing on the basis of API specifications expressed with [OpenAPI](https://github.com/OAI/OpenAPI-Specification). We
rely on Testcontainers to run the mock and execute tests against it leveraging [REST-assured](https://rest-assured.io).
This approach allows for consistent and automated integration testing both local and in CI/CD pipelines.

## Illustrated Microcks Features

By means of our example API (see below), we are able to showcase several of Microcks's core features:
- [Microcks Testcontainers module](https://testcontainers.com/modules/microcks)
- [Multi-artifacts](https://microcks.io/documentation/explanations/multi-artifacts)
- Dispatchers:
[Inferred URI PARTS dispatcher](https://microcks.io/documentation/explanations/dispatching/#inferred-dispatchers),
[JSON BODY dispatcher](https://microcks.io/documentation/explanations/dispatching/#json-body-dispatcher), and
[SCRIPT dispatcher](https://microcks.io/documentation/explanations/dispatching/#script-dispatcher) with
[context expressions](https://microcks.io/documentation/references/templates/#context-expression)
- [Dynamic mock content](https://microcks.io/documentation/explanations/dynamic-content) with
[function expressions](https://microcks.io/documentation/references/templates/#function-expressions)
- Programmatic usage of [Microcks's own API](https://microcks.io/documentation/references/apis/open-api) for mock
preparation beyond characteristics from API specifications

## Running the Example
The example requires Maven and Java 21 or greater, and consists of a
[test suite](src/test/java/de/codecentric/microcks_demo/TestSuite.java) with a total of 14 REST-assured
API tests, which are executed against a Microcks testcontainer, whose preparation routine is implemented in the
[`InfrastructureSetup` class](src/test/java/de/codecentric/microcks_demo/tests/infrastructure/InfrastructureSetup.java).

To spin up the testcontainer and execute the tests run `mvn clean test` from the application root directory in which the
[`pom.xml`](pom.xml) file resides.

It is also possible to inspect the prepared mock in the corresponding testcontainer by means of the command
`mvn clean test -Dtest="InfrastructureSetup"`. It prints the mock's URI, which can be opened in a local browser, to the
terminal and pauses the application for browser-based mock examination until Ctrl+c is hit.

## The Example API

The demo application comprises an [OpenAPI specification for an example API](src/test/resources/customers.yaml) that is
loaded into a Microcks testcontainer and accompanied with examples from a
[dedicated Postman collection](src/test/resources/customers_examples.postman_collection.json).

**Disclaimer:** We discovered this API in the wild, and found it quite coherent, understandable, and therefore suitable
to illustrate some of Microcks core features. However, it also bears quite some potential for optimization and does not
reflect a style of API design we advise for productive usage.

The example API is rooted in the domain of Customer Relationship Management and provides the following operations:
 
- `GET /customer_kinds`  
Retrieve information about the supported kinds of customers. For its execution, this
operation expects a fixed API key in the form of a [JSON Web Token (JWT)](https://jwt.io) to be provided upon calling
the operation as value for the HTTP header field `api_key`. An example response to a valid operation call looks as
follows:
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
Log in a customer or other user. This operation requires the API key token in the same fashion as `GET /customer_kinds}`
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