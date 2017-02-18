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
This is less explicit, but the GAV coordinates are common enough, nowadays.
In XML this could look like this:

```xml
<dependency>ch.qos.logback:logback-classic:1.2.1</dependency>
```

While this makes the syntax more concise, the real benefit other languages _can_ provide (if used properly),
is to reduce the repetition in you build files - Don't Repeat Yourself or DRY.
By leveraging reuse, build files don't just get more concise,
they also get more uniform (by reducing the snowflake effect)
and more expressive (if you can find a solid abstraction with a clear name).
I.e. if you want e.g. a Java EE 7 WAR, you should only specify that this is what you want,
and the know-how required to build it, the dependencies, the Java compiler setting, the properties, etc.
is expressed once-and-only-once in a file in the repository. We can do that with XML.
In this project, I do so by allowing profiles to be defined in Maven repository files.

Xml is not the problem.


## Quick-Start

Add a directory `.mvn` to your project.
Create a file `extensions.xml` in `.mvn` containing:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>com.github.t1</groupId>
        <artifactId>pomx</artifactId>
        <version>VERSION</version>
    </extension>
</extensions>
```

Copy your `pom.xml` to `pomx.xml` and stop using the old file... it will be overwritten with every build.
It's required by your IDE and other tools and Maven will install and deploy it normally,
so Repository managers etc. can use it.
You don't have to put it under version control, but a fresh checkout would not be properly recognized by your IDE.


## Setup

I took a look at the code of the Polyglot project, but I absolutely need a classic POM to be generated
for other tools to work correctly; Polyglot still works on this.
And my code is much smaller and easier to understand.

The XSD is in `https://raw.githubusercontent.com/t1/pomx/master/src/main/resources/schemas/pom-5.0.0.xsd`
and I boldly name the namespace `urn:xsd:maven:pomx:5.0.0`.


## Features

### Include `modelVersion` & New XSD

old:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
</project>
```

new:

```xml
<project xmlns="urn:xsd:maven:pomx:5.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="urn:xsd:maven:pomx:5.0.0 https://raw.githubusercontent.com/t1/pomx/master/src/main/resources/schemas/pom-5.0.0.xsd">
</project>
```


### Compact GAV with packaging as element

old:

```xml
<project>
    <groupId>com.github.t1</groupId>
    <artifactId>deployer</artifactId>
    <version>2.7.3-SNAPSHOT</version>
    <packaging>war</packaging>
</project>
```

new:

```xml
<war>com.github.t1:deployer:2.7.3-SNAPSHOT</war>
```

schema: `<packaging>groupId:artifactId:version</packaging>`
or: `<packaging>groupId:artifactId:classifier:version</packaging>`


### Compact GAV of Build Plugins

old:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>2.4</version>
    <configuration>
        <archive>
            <addMavenDescriptor>false</addMavenDescriptor>
        </archive>
    </configuration>
</plugin>
```

new:

```xml
<plugin id="org.apache.maven.plugins:maven-jar-plugin:2.4">
    <configuration>
        <archive>
            <addMavenDescriptor>false</addMavenDescriptor>
        </archive>
    </configuration>
</plugin>
```


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

Profiles can be stored in a repository and referenced in the POMX:

```xml
<profile>javax:javaee-api:7.0</profile>
```

The profile xml file is resolved like a maven dependency, included into the POM, and activated when run.
Even though it is a `profile`, the file can have a `project` namespace like a `pomx`, i.e.:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<project xmlns="urn:xsd:maven:pomx:5.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="urn:xsd:maven:pomx:5.0.0 https://raw.githubusercontent.com/t1/pomx/master/src/main/resources/schemas/pom-5.0.0.xsd">
    <pom>my.group:my.artifact:1.0</pom>
    ...
</project>
```

In the generated POM, this profile would have the `<id>my.group:my.artifact</id>` so you can disable it like this:

`mvn clean install -P-my.group:my.artifact`

(The colon was chosen for clarity, as '.' and '-' are too common in group and artifact ids)

Some profile elements are removed and not copied into the target profile: `modelVersion`, `groupId`, `artifactId`, `version`, `packaging`, `name`, `description`

Some profile elements are merged into the target `project`, not copied into the target profile: `licenses`, `repositories`, `distributionManagement`
You can't deactivate these elements by deactivating the profile.

# TODO

External profiles are yet expanded only from your local repository (`~/.m2/repository`).
So you'll have to fetch them manually before you start the first build:

`mvn dependency:get -DgroupId=my.group -DartifactId=my.artifact -Dpackaging=xml -Dversion=1.0`
