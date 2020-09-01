package net.sourcebot.module.trivia

import net.sourcebot.Source
import net.sourcebot.api.module.SourceModule
import net.sourcebot.module.trivia.command.TriviaCommand
import net.sourcebot.module.trivia.data.TriviaGameManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class Trivia : SourceModule() {

    override fun onEnable(source: Source) {
        source.commandHandler.registerCommands(this, TriviaCommand(TriviaGameManager(source, this)))
    }

    companion object {
        val executorService: ExecutorService = Executors.newFixedThreadPool(2)
        val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    }
}