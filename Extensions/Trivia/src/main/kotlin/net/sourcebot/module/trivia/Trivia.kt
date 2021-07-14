package net.sourcebot.module.trivia

import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.trivia.command.TriviaCommand
import net.sourcebot.module.trivia.data.TriviaListener

class Trivia : SourceModule() {

    override fun enable() {
        TRIVIA_LISTENER = TriviaListener()
        registerCommands(TriviaCommand())
        subscribeEvents(TRIVIA_LISTENER)
    }

    companion object {
        @JvmStatic
        internal lateinit var TRIVIA_LISTENER: TriviaListener
    }
}