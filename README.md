[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.tk.opensource/3rdparty-privacy-proxy/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/de.tk.opensource/3rdparty-privacy-proxy)
 [![Build Status](https://travis-ci.org/DieTechniker/3rdparty-privacy-proxy.svg?branch=master)](https://travis-ci.org/DieTechniker/3rdparty-privacy-proxy) [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=DieTechniker_3rdparty-privacy-proxy&metric=alert_status)](https://sonarcloud.io/dashboard?id=DieTechniker_3rdparty-privacy-proxy)

# TK 3rd Party Privacy Proxy 

Deliveres functionality to securely fetch and provide 3rd Party resources as well as proxying requests back to the 3rd Party Provider. The users of your website will remain private against any 3rd Party Provider you use without losing any functionality on your end. Further more this privacy proxy delivers you the ultimate knowledge about what information are being transferred to any 3rd Party Provider as well as independence from their servers.

## Getting Started

This software is built with JAVA / Spring Boot. You do require a fully working Java environment to make it running. You can simply build this project with maven to get an executable JAR which contains all you need. Customizing the mvn file you could also create a war. Your choice. For most of you the JAR will just do fine. 

This ist just the library to deliver the base functionality. To implement your own privacy secured retrieval and routing services clone the example project from here: https://github.com/DieTechniker/3rdparty-privacy-proxy-examples

**If you want to contribute to the base library, continue reading. If you want to use this tool, please continue in the example repo linked above**

### Prerequisites

To build and run this software, you require
* A current version of [Maven](https://maven.apache.org/)
* A current version of the [OpenJDK](https://developers.redhat.com/products/openjdk/download/)

This project is configured against Java 8. So for the time being you should go with that.

### Installing

To build an executable **JAR** to deploy on your server use

```
mvn install
```
and then start it with
```
java -jar target/3rdparty-privacy-proxy.jar
```

To simply **run the application locally** to test, develop or check it out, run
```
mvn spring-boot:run 
```
The configuration relies on **profiles** to determine which configuration block should be used. To start the application with a specific profile ("dev" in this example) use
```
mvn spring-boot:run -Drun.profiles=dev
or
java -jar -Dspring.profiles.active=dev target/3rdparty-privacy-proxy.jar
```

## Deployment

Use the Maven Central dependency within your code.

## Configuration

All configuration is done within the ```application.yml``` config file of your implementation code. Your code will use this library as a dependency. So do not change something within this base code.

## Built With

* [SpringBoot](http://spring.io/projects/spring-boot)
* [Maven](https://maven.apache.org/)

## Authors

* **Kassim Hölting** - *Contributing to the v1.0 release and bringing it to Maven Central*
* **Artur Baron** - *Writing this first version of the base library*
* **Benjamin Stark** - *Making the first open source version of the base library ready to appear*
* **Jan Thiel** - *Developing the idea behind this project, driving it's development and writing the docs*

## License

This project is licensed under the GPLv3 License - see the [LICENSE](LICENSE) file for details

## TK OpenSource

As a legal public entity we work hard to deliver the best service to our customers. As we work in the interest of the public, we decided to OpenSource stuff we code which might be interesting to the public. Check out our [GitHub Page](https://github.com/DieTechniker/) for the latest releases. Everything is as it is. Feel free to fork or open Pull Requests if you want to enhance our solutions.
