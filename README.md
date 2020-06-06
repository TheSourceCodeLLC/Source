# Source V5

How to use:

* Compile Source on your local machine:
  - `git clone git@github.com:The-SourceCode/SourceV5.git`
  - `cd SourceV5`
  - `mvn install -DtargetRoot=.\target`
* Create a folder somewhere for the bot to run in (runtime dir)
* Copy `API-v5.jar` from `target/bin/Source` to the runtime dir.
* Create a folder named `modules` in the runtime dir (module dir)
* Copy any modules you want to run into the module dir. 
* Copy `config.example.json` to the runtime dir, and rename it to `config.json`
* Configure the bot to your liking.
* Start the bot, i.e `java -jar API-v5.jar`

# Using Permissions
The first time the bot starts; you won't be able to set permissions using commands.
You will need to modify user data on MongoDB in order to grant yourself the permission `setpermission`, otherwise you will have to change permissions via MongoDB every time.
After setting a permission in MongoDB; it is necessary to restart the bot. If you do it right, you will only need to do this once.

Once someone has the permission to use the `setpermission` command, it will no longer be necessary to log into the database to change the value manually.

To set the permission via MongoDB, you must add the following object element to the respective `permissions` object array; either on an entry in the `role-permissions` or `user-permissions` collection:
```json
{
  "node": "setpermission",
  "flag": "true"
}
```

Make sure the role ID or user ID you are updated corresponds to the proper one in your Discord server.

   