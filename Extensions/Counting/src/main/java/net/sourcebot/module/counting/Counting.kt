package net.sourcebot.module.counting

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.api.properties.JsonSerial
import net.sourcebot.module.counting.command.CountingCommand
import net.sourcebot.module.counting.data.CountingDataController
import net.sourcebot.module.counting.data.CountingDataController.CountingData
import net.sourcebot.module.counting.data.CountingListener
import java.io.File

class Counting : SourceModule() {
    private lateinit var dataController: CountingDataController
    override fun onEnable(source: Source) {
        val dataFile = File(dataFolder, "data.json")
        if (!dataFile.exists()) {
            dataFile.createNewFile()
            JsonSerial.mapper.writeValue(dataFile, HashMap<String, CountingData>())
        }
        dataController = CountingDataController(dataFile)
        CountingListener(source, dataController).listen(this)
        registerCommands(CountingCommand(dataController))
    }

    override fun onDisable(source: Source) {
        dataController.save()
    }
}