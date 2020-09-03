package net.sourcebot.module.trivia.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.RestAction
import net.sourcebot.Source
import net.sourcebot.api.response.EmbedResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.trivia.Trivia
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Game(amount: Int, category: Int?) {
    private val triviaListener = Trivia.triviaListener
    private val questions = LinkedList(
        OpenTDB.requestQuestions(amount, category)
    )
    private val totalRounds = questions.size
    private val answers = LinkedHashMap<String, Int>()
    private val scores = HashMap<String, Int>()

    private lateinit var postGame: () -> Unit
    private lateinit var tickFuture: ScheduledFuture<out Any>
    private lateinit var message: Message
    private lateinit var current: OpenTDB.Question
    private var currentRound = 1

    internal fun setMessage(message: Message) {
        this.message = message
        triviaListener.link(message.id, answers)
    }

    fun getJumpUrl() = message.jumpUrl

    fun start(postGame: () -> Unit): Response {
        this.postGame = postGame
        tickFuture = Source.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
            this::gameTick, 10, 30, TimeUnit.SECONDS
        )
        return TriviaStartResponse()
    }

    class TriviaStartResponse : InfoResponse(
        "Trivia Game Starting!",
        "A new game of Trivia will begin in 10 seconds."
    )

    var firstTick = true
    private fun gameTick() {
        var firstBonus = true
        answers.forEach { (user, answer) ->
            val score = scores.computeIfAbsent(user) { 0 }
            if (current.answers[answer].correct) {
                var toIncrement = 1
                if (firstBonus) {
                    toIncrement += 1
                    firstBonus = false
                }
                scores[user] = score + toIncrement
            }
        }
        answers.clear()
        current = questions.poll() ?: return stop()
        message.editMessage(
            QuestionResponse(
                currentRound++,
                totalRounds,
                current
            ).asEmbed(message.author)
        ).queue()
        if (firstTick) {
            validEmotes.map(message::addReaction).forEach(RestAction<Void>::queue)
            firstTick = false
        }
    }

    fun stop() {
        message.editMessage(
            ScoreResponse(
                scores.entries.sortedByDescending { (_, v) -> v }.take(5)
            ).asEmbed(message.author)
        ).queue { it.clearReactions().queue() }
        triviaListener.unlink(message.id)
        postGame()
        tickFuture.cancel(true)
    }

    class ScoreResponse(
        topFive: List<Map.Entry<String, Int>>
    ) : SuccessResponse(
        "Trivia Over!"
    ) {
        init {
            description = if (topFive.isEmpty()) "Nobody played this game of Trivia!"
            else topFive.joinToString("\n") { (id, score) -> "<@$id>: $score" }
        }
    }

    class QuestionResponse(
        round: Int,
        totalRounds: Int,
        question: OpenTDB.Question
    ) : EmbedResponse(
        "Round $round of $totalRounds",
        """
            [${question.category}] [${question.difficulty}]
            ${question.text}
        """.trimIndent()
    ) {
        init {
            val (q1, q2, q3, q4) = question.answers
            addField("", "\uD83C\uDDE6 **${q1.text}**", false)
            addField("", "\uD83C\uDDE7 **${q2.text}**", false)
            addField("", "\uD83C\uDDE8 **${q3.text}**", false)
            addField("", "\uD83C\uDDE9 **${q4.text}**", false)
        }
    }
}