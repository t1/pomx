### XSD incl. version

old:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
</project>
```

new:

```xml
<project xmlns="urn:xsd:pom" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="urn:xsd:pom http://maven.apache.org/xsd/maven-5.0.0.xsd">
</project>
```

### Compact GAV with packaging as element

old:

```xml
<groupId>com.github.t1</groupId>
<artifactId>deployer</artifactId>
<version>2.7.3-SNAPSHOT</version>
<packaging>war</packaging>
```

new:

```xml
<war>com.github.t1:deployer:2.7.3-SNAPSHOT</war>
```

schema: `<packaging>groupId:artifactId:version</packaging>`
or: `<packaging>groupId:artifactId:classifier:version</packaging>`


### Compact Dependency Management

old:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.jboss.arquillian</groupId>
                <artifactId>arquillian-bom</artifactId>
                <version>1.1.11.Final</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

new:

```xml
    <dependencyManagement>
        <pom>org.jboss.arquillian:arquillian-bom:1.1.11.Final</pom>
    </dependencyManagement>
```


### Compact Dependencies Grouped By Scope

old:

```xml
    <dependencies>
        <dependency>
            <groupId>javax</groupId>
            <artifactId>javaee-api</artifactId>
            <version>7.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

new:

```xml
    <dependencies>
        <provided>
            <jar>javax:javaee-api:7.0</jar>
        </provided>
    </dependencies>
```

`optional`:
```xml
<jar optional="true">javax:javaee-api:7.0</jar>
```


### External Profiles

Profiles can be stored in a repository

```xml
<profile>javax:javaee-api:7.0</profile>
```


### Conditions

Some parts can be switched on a condition (similar to profile activation):

```xml
<if packaging="war">
    ...
</if>
```

Currently supported conditions:
* `packaging="T"`

