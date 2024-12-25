# Room Reservation Search
Function app exposing API endpoints for oom reservation search from cosmos DB store.

This function app code is part of overall room reservation search solution described here: https://mgmridev.atlassian.net/wiki/spaces/UCP/pages/936017944/Room+Reservation+Search

## Swagger - API Documentation
https://mgmdigitalventures.atlassian.net/wiki/spaces/UCP/pages/257327232/Reservation+Search+Services

## Running Application Locally

### Prerequisites
 - Java, version 8
 - Apache Maven, version 3.0 or above
 - Azure Function Core Tools (https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local#v2)
 
### Package
```
mvn clean package
```

### Run
```
mvn azure-functions:run
```

## Dependencies
Code in this repository depends on following common repositories:

 - https://github.com/MGMResorts/booking-opera-interface-models: Repository holds models from different opera interfaces like OWS and OXI reused across different function apps.
 - https://github.com/MGMResorts/booking-common-error: Repository contains common code related to error framework/classes
 - https://github.com/MGMResorts/booking-spring-cloud-common: Repository containing some extensions to spring cloud implementation and encapsulates common functionalities like live site events, generating error responses etc.

## Enable logging locally
To enable logging, create a copy of the file 'log4j2-local.xml' and rename to 'log4j2-test.xml' in the resources folder.
