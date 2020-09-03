package net.sourcebot.module.trivia

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.trivia.command.TriviaCommand
import net.sourcebot.module.trivia.data.TriviaListener


class Trivia : SourceModule() {

    override fun onEnable(source: Source) {
        triviaListener = TriviaListener(this, source.jdaEventSystem)
        source.commandHandler.registerCommands(this, TriviaCommand())
    }

    companion object {
        @JvmStatic
        lateinit var triviaListener: TriviaListener
    }
}