# Source V5

Source V5 API and Official Modules are available via GitHub Packages.
GitHub Packages may not be maintained! Use at your own risk!

# Build Instructions:
Note: You need Java 11 to compile and run Source.

* Compile Source on your local machine:
  - `git clone git@github.com:The-SourceCode/SourceV5.git`
  - `cd SourceV5`
  - `mvn install -DtargetRoot=/path/to/SourceV5/target`
* Create a folder somewhere for the bot to run in (runtime dir)
* Copy `API-v5.jar` from `target/bin/Source` to the runtime dir.
* Create a folder named `modules` in the runtime dir (module dir)
* Copy any modules you want to run into the module dir. 
* Copy `config.example.json` to the runtime dir, and rename it to `config.json`, or start the bot to pregenerate it.
* Configure the bot to your liking.
* Start the bot:
  * Windows: `java -DuseJansi=true -jar API.jar`
  * Unix: `java -jar API.jar`

# Using Permissions
To enable permission modification via commands, you must install the Permissions module

To set permissions with commands, you need permission for the `setpermission` command.

You will need to modify user data on MongoDB to set this permission.
After setting a permission in MongoDB; it is necessary to restart the bot. 
If you do it right, you will only need to do this once.

To set the permission via MongoDB, you must add the following object element to the respective `permissions` object array; either on an entry in the `role-permissions` or `user-permissions` collection:
```json
{
  "node": "setpermission",
  "flag": true
}
```

Alternatively, you can grant access to ALL permissions with the following object:
```json
{
  "node": "*",
  "flag": true
}
```

Make sure the role ID or user ID you are updated corresponds to the proper one in your Discord server.

   