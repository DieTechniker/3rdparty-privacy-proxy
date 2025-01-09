# Release notes
## 1.2.1
* Stopped logging http error stacktraces to warn.
## 1.2.0
* Upgrade to Spring-Boot 3.0.8
* Upgrade to Java 17
## 1.1.0
* Updated to Spring-Boot 2.7.6
* Dependencies maintained
* Using env https.\*proxy\* only for proxy setup
* Deprecated Code deleted
* in some cases switched to semantic return status codes, for example a bad request
## 1.0.15
* Upgrade to Spring-Boot 2.7.1
* Modified GitHub Action to build maven snapshots on published features 
* RoutingHandler Refactoring
* Added generic RoutingHandler.handleGenericRequestInternal() to handle GET and POST via Spring RestTemplate including the request body. Legacy RoutingHandler logic is deprecated.
* Rearranged helper bean initialization to configuration 
* switched to null checks on http proxy properties
* Added spring-boot-starter-test and hamcrest-all test dependencies
* Moved from org.junit to org.junit.jupiter.api
* Implemented more tests
