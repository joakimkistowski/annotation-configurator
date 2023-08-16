# annotation-configurator

Simple tiny library for configuration injection using annotations.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.joakimkistowski/annotation-configurator.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.joakimkistowski%22%20AND%20a:%22annotation-configurator%22)
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/joakimkistowski/annotation-configurator/Release/main)
![GitHub](https://img.shields.io/github/license/joakimkistowski/annotation-configurator)

## How to use

Get the library from maven central.
```xml
<dependency>
    <groupId>io.github.joakimkistowski</groupId>
    <artifactId>annotation-configurator</artifactId>
    <version>0.2.2</version>
</dependency>
```

1. Annotate your class attributes with the `@Config`-annotation.

```java
public class MyExampleConfiguration {

    @Config("CONFIGURATION_NAME")
    private String configuration = "My default configuration";

    // additional code ...
}
```

2. (**OPTIONAL**) Create at least one `.properties` file in your classpath to load the configuration from.   
  You may also specify configuration parameters to be read as environment variables. In case a configuration is defined in a `.property` file and in an environment variables, the environment variable will override the configuration of the file.
3. Inside your configuration class: Create and call an `AnnotationPropertyConfigContext` reading your property-file.

```java
// Create the context
new AnnotationPropertyConfigContext(List.of("MY_PROPERTY_FILE.properties"))
        
    // Read the property files and configurations from environment variables.
    .configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(

        // Function for setting the configuration class attributes. This function is defined in your current class and, therefore, has access priviledges to all private members of the current class.
        this.getClass(), (field, value) -> field.set(this, value)
    );
```

## Supported Configuration Property Types

The following types are supported and may be annotated with a `@Config` annotation:

* `String`
* `int` or `Integer`
* `double` or `Double`
* `float` or `Float`
* `boolean` or `Boolean`
* Any `enum`
* Any `List` of the above: Lists are parsed from comma-separated properties or environment variables

## Loading Configurations On-Demand

Specifying different configurations for a local development and your production environment is a common use case.

The `annotation-configurator` supports two methods for specifying different configurations that may be loaded on demand:
* Specifying a prioritized list of property files that may or may not be present, depending on environment.
* Specifying a property-file to be loaded in an environment variable.


At the core of both methods is the following behavior:
1. The `annotation-configurator` loads property files from an ordered list, where later properties override earlier ones.
2. The `annotation-configurator` gracefully skips files with `null` or empty file names and gracefully skips missing files.

### Specifying a Prioritized List of Property Files

The configurator context loads property files from an ordered list, where later properties override earlier ones. In addition, missing files are simply skipped.

As a result, you can load property files as follows:

```java
new AnnotationPropertyConfigContext(List.of(
        "MY_REGULAR_PROPERTY_FILE.properties",
        "PROPERTY_FILE_THAT_IS_NOT_PACKAGED_BY_MAVEN_OR_GRADLE.properties",
        "LOCAL_PROPERTY_FILE_IGNORED_BY_GIT.properties"
    )).configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
        this.getClass(), (field, value) -> field.set(this, value)
    );
```

### Specifying a Property-File to be Loaded in an Environment Variable

Similar to the "profile"-mechanism of large Java frameworks, you may specify the name of a file in an environment variable. If present the properties in this file will then override the properties of other files.

```java
new AnnotationPropertyConfigContext(List.of(
        "MY_REGULAR_PROPERTY_FILE.properties",
        System.getenv("PROFILE_SPECIFIC_PROPERTY_FILE_NAME")
    )).configureAttributesInThisClassUsingPropertyFilesAndEnvironmentVariables(
        this.getClass(), (field, value) -> field.set(this, value)
    );
```

