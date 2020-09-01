package net.sourcebot.module.trivia.data

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.module.trivia.Trivia

class TriviaGameManager(private val source: Source, private val trivia: Trivia) {
    val categories: Categories by lazy {
        Categories.fetchCategories()
    }

    private val activeGames = hashMapOf<Long, TriviaGame>()

    fun prepareGame(category: Categories.Category, amount: Int, difficulty: String, message: Message) {
        activeGames.computeIfAbsent(message.guild.idLong) {
            val options = GameOptions(category, amount, difficulty)
            val game = TriviaGame(options, message.channel, source, trivia, this)
            game.setup()
            game
        }
    }

    fun finishGame(guild: Long) {
        activeGames.remove(guild)
    }
}