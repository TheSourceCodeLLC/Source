package net.sourcebot.module.tags

import net.sourcebot.Source
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.module.SourceModule
import org.bson.Document

class Tags : SourceModule() {
    override fun onEnable(source: Source) {
        MongoSerial.register(TagHandler.Tag.Serial())

        val mongodb = source.mongodb
        val tags = mongodb.getCollection<Document>("tags")
        val prefix: String = config.required("prefix")
        val tagHandler = TagHandler(tags, prefix)

        source.jdaEventSystem.listen(this, tagHandler::onMessageReceived)
        registerCommands(TagsCommand(tagHandler))
    }
}