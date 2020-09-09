package net.sourcebot.module.trivia

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.trivia.command.TriviaCommand
import net.sourcebot.module.trivia.data.TriviaListener

class Trivia : SourceModule() {

    override fun onEnable() {
        triviaListener = TriviaListener(this, source.jdaEventSystem)
        registerCommands(TriviaCommand())
    }

    companion object {
        @JvmStatic
        lateinit var triviaListener: TriviaListener
    }
}