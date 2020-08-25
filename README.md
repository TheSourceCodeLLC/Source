# Source V5

# Build Instructions:
1. You need Java 11 run Source. </br>
2. You need Gradle to compile source. </br>
  - If you do not have Gradle installed, it is recommended to compile using an IDE with support for Gradle projects.
  
* Compile Source on your local machine:
  - `git clone git@github.com:TheSourceCodeLLC/Source.git`
  - `cd Source`
  - `gradle install`
* Binary files (Main JAR & Modules) are located in the `target/bin` folder

NOTE: It is recommended to let Source have full reign over a separate mongodb instance! <br>
Source needs the ability to create and delete databases at will as to separate guild data! <br>
If you do not have the option to do this, **Source may be a poor choice for you!** <br>

NOTE: Source uses privileged gateway intents. <br>
You will need to activate the two privileged Gateway Intents for Source to function. <br>
To do this, you will need to activate to the Discord Developers Portal, under the 'Bot' section. <br>

* Start the bot:
  * Windows: `java -DuseJansi=true -jar Source.jar`
  * Unix: `java -jar Source.jar` </br>
If you need to configure the bot, the program will terminate after generating a default config.

# Using Permissions
Members that permissions do not apply to: <br>
-  Members in a server that have `ADMINISTRATOR` permissions
-  Global Admins as defined in the `config.json`

Global Admins are the ONLY people who may use commands marked as global. <br>
Command Breakdown:
```
permissions:
  <user|role>: The type of entity to modify
    <target>: The entity to modify
        set: Set a permission for this entity
          <toSet>: The permission node to set
          <flag>: The flag for the new node
          (context): An optional context to set in
        unset: Unset a permission for this entity
          <toUnset>: The permission node to unset
          (context): An optional context to unset from
        info: Get permission info for this entity
        check:
          <toCheck>: The permission node to check
          (context): An optional context to check in
        clear: Clear this entity's permissions
          (context): An optional context to clear in
```