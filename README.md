# Xml Is Not The Problem

There's a lot of complaining about [Maven](http://maven.apache.org) being too verbose.
The Maven developers listened and started [Polyglot Maven](https://github.com/takari/polyglot-maven/tree/master/polyglot-xml)
to address the issue.
They support Groovy, Scala, Ruby, Yaml or other file formats, and they may be beneficial to some people,
but I don't think that XML is the problem, so those other file formats are not guaranteed to be a cure.

XML has reputation for being verbose, because requires you to repeat the name of the opening tag when closing it.
This actually does add a bit of verbosity, but it's also quite useful to orient in large blocks.
XML also supports attributes, which require less overhead than JSON!
Maven POM files just never use them.
Instead of expressing a dependency like this:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.1</version>
</dependency>
```

A POM could use attributes like this:

```xml
<dependency groupId="ch.qos.logback" artifactId="logback-classic" version="1.2.1"/>
```

The Polyglot project has an XML format that goes exactly this way.
The other file formats use an even more compact format by simply separating the GAV fields with colons.
In XML this could look like this:

```xml
<dependency>ch.qos.logback:logback-classic:1.2.1</dependency>
```

While this makes the syntax more concise, the real benefit other languages _can_ provide (if used properly),
is to reduce the repetition in you build files.
By leveraging reuse, build files don't just get more concise, they also get more uniform and more expressive.
I.e. if you want e.g. a Java EE 7 WAR, you should only specify that this is what you want,
and the know-how required to build it, the dependencies, the Java compiler setting, the properties, etc.
is expressed once-and-only-once in a file in the repository. We can do that with XML.
In this project, I do so by allowing profiles to be defined in Maven repository files.

Xml is not the problem.


## Setup

I took a look at the code of the Polyglot project, but I absolutely need a classic POM to be generated
for other tools to work correctly; Polyglot still works on this.
And my code is much smaller and easier to understand.

I think I should make it a Maven extension, and I still need to write an XSD.


## Features

### Include `modelVersion`

old:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:xsd:pom"
         xsi:schemaLocation="urn:xsd:pom http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
</project>
```

new:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:xsd:pom"
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


### External Profiles (TODO)

Profiles can be stored in a repository

```xml
<profile>javax:javaee-api:7.0</profile>
```


### Conditions (TODO)

Some parts can be switched on a condition (similar to profile activation):

```xml
<if packaging="war">
    ...
</if>
```

Currently supported conditions:
* `packaging="T"`

