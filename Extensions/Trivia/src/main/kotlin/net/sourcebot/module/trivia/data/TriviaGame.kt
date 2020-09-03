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
    private lateinit var message: Message
    private lateinit var current: OpenTDB.Question
    private var currentRound = 0

    private lateinit var lastTick: ScheduledFuture<out Any>

    internal fun setMessage(message: Message) {
        this.message = message
        triviaListener.link(message.id, answers)
    }

    fun getJumpUrl() = message.jumpUrl

    fun start(postGame: () -> Unit): Response {
        this.postGame = postGame
        //Dispatch initial tick after 10 seconds
        lastTick = Source.SCHEDULED_EXECUTOR_SERVICE.schedule(
            this::gameTick, 10, TimeUnit.SECONDS
        )
        return TriviaStartResponse()
    }

    class TriviaStartResponse : InfoResponse(
        "Trivia Game Starting!",
        "A new game of Trivia will begin in 10 seconds."
    )

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
        if (this::current.isInitialized) {
            message.editMessage(
                PostRoundResponse(
                    currentRound,
                    totalRounds,
                    current,
                    getTopFive(),
                    questions.size > 0
                ).asEmbed(message.author)
            ).complete()
            message.clearReactions().complete()
            Thread.sleep(5000)
        }
        //If there are no more questions, do not re-tick
        current = questions.poll() ?: return stop()
        message.editMessage(
            QuestionResponse(
                ++currentRound,
                totalRounds,
                current
            ).asEmbed(message.author)
        ).queue {
            validEmotes.map(message::addReaction).forEach(RestAction<Void>::queue)
            //Re-tick after each question is posted
            lastTick = Source.SCHEDULED_EXECUTOR_SERVICE.schedule(
                this::gameTick, 20, TimeUnit.SECONDS
            )
        }
    }

    fun stop() {
        message.editMessage(
            ScoreResponse(getTopFive()).asEmbed(message.author)
        ).queue { it.clearReactions().queue() }
        triviaListener.unlink(message.id)
        postGame()
        lastTick.cancel(true)
    }

    private fun getTopFive() = scores.entries.sortedByDescending { (_, v) ->
        v
    }.take(5)

    class PostRoundResponse(
        round: Int,
        totalRounds: Int,
        question: OpenTDB.Question,
        topFive: List<Map.Entry<String, Int>>,
        hasNext: Boolean
    ) : InfoResponse(
        "Round $round of $totalRounds",
        """
            [${question.category}] [${question.difficulty}]
            ${question.text}
            
            ${if (hasNext) "Next round" else "Game ends"} in 5 seconds.
        """.trimIndent()
    ) {
        init {
            addField("Correct Answer:", question.correct, false)
            addField("Current Scores:", topFive.joinToString("\n") { (id, score) ->
                "<@$id>: $score"
            }, false)
        }
    }

    class ScoreResponse(
        topFive: List<Map.Entry<String, Int>>
    ) : SuccessResponse(
        "Trivia Over!",
        "The game of Trivia has concluded."
    ) {
        init {
            val scores = if (topFive.isEmpty()) "Nobody played this game of Trivia!"
            else """
                ${topFive.joinToString("\n") { (id, score) -> "<@$id>: $score" }}
            """.trimIndent()
            addField("Final Scores:", scores, false)
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