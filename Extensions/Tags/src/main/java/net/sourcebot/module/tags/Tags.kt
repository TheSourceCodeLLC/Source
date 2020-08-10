package net.sourcebot.module.tags

import net.sourcebot.Source
import net.sourcebot.api.database.MongoSerial
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.tags.command.TagsCommand
import net.sourcebot.module.tags.data.Tag
import net.sourcebot.module.tags.data.TagHandler

class Tags : SourceModule() {
    override fun onEnable(source: Source) {
        MongoSerial.register(Tag.Serial())

        val tagHandler = TagHandler(source.mongodb, config.required("prefix"))

        source.jdaEventSystem.listen(this, tagHandler::onMessageReceived)
        registerCommands(TagsCommand(tagHandler))
    }
}