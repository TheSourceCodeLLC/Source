# Source V5

# Build Instructions:
**Notice:** You need Java **11** to run Source. </br>

* Clone Source to your local machine:
  - `git clone https://github.com/TheSourceCodeLLC/Source.git`
  - `cd Source`
* Tell the compiler what modules to install (optional):
  - Rename `INSTALL.example` to `INSTALL`
  - Modify the `INSTALL` file to change what modules will be installed.__
  - Note: Module names that are not in this list will not be installed.
* Compile Source and its modules:
  - `./gradlew install`
* Binary files (Main JAR & Modules) are located in the `target/bin` folder

**NOTE:** It is recommended to let Source have full reign over a separate MongoDB instance! <br>
Source needs the ability to create and delete databases at will as to separate Guild data! <br>
If you do not have the option to do this, **Source may be a poor choice for you!** <br>

**NOTE:** Source uses privileged gateway intents. <br>
You will need to activate the two privileged Gateway Intents for Source to function. <br>
To do this, you will need to activate to the Discord Developers Portal, under the 'Bot' section. <br>

* Start the bot:
  * Windows: `java -DuseJansi=true -jar Source.jar`
  * Unix: `java -jar Source.jar` </br>
If you need to configure the bot, the program will terminate after generating a default configuration.

# Using Permissions
Members that permissions do not apply to: <br>
-  Members in a server that have `ADMINISTRATOR` permissions
-  Global Admins as defined in the `config.json`

Global Admins are the ONLY people who may use commands marked as global. <br>

# Base Module Information
Configuration Values:
  - `source.connections.channel`: The channel ID join / leave messages will be sent to.
  - `source.connections.joinMessages`: Messages to be sent when Members join
  - `source.connections.leaveMessages`: Messages to be sent when Members leave
  
# Artifact Information
Note: `x.y.z` must be replaced with the version of Source you wish to use!

## Gradle
```kotlin
repository {
  maven("https://nexus.dveloped.net/repository/dveloped/")
}

dependency {
  compileOnly("net.sourcebot:API:x.y.z")
}
```

## Maven
```xml
<repositories>
  <repository>
    <id>dveloped</id>
    <url>https://nexus.dveloped.net/repository/dveloped/</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>net.sourcebot</groupId>
    <artifactId>API</artifactId>
    <version>x.y.z</version>
    <type>jar</type>
    <scope>provided</scope>
  </dependency>
</dependencies>
```