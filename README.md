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
   