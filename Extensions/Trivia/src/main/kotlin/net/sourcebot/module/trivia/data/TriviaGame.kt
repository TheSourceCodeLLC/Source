package net.sourcebot.module.trivia.data

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.RestAction
import net.sourcebot.Source
import net.sourcebot.api.formatLong
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardEmbedResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.trivia.Trivia
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Game(private val amount: Int, private val category: Int?) {
    private val log = Collections.synchronizedList(ArrayList<String>())
    private val triviaListener = Trivia.TRIVIA_LISTENER
    private val questions = LinkedList(
        OpenTDB.requestQuestions(amount, category)
    )
    private val totalRounds = questions.size
    private val answers = LinkedHashMap<String, Int>()
    private val scores = HashMap<String, Int>()

    private lateinit var postGame: () -> Unit
    private lateinit var message: Message
    private var current: OpenTDB.Question? = null
    private var currentRound = 0

    private lateinit var lastTick: ScheduledFuture<out Any>

    internal fun setMessage(message: Message) {
        this.message = message
        triviaListener.link(message, answers)
    }

    fun getChannel() = "<#${message.channel.id}>"

    fun start(postGame: () -> Unit): Response {
        this.postGame = postGame
        log.add("Trivia Started")
        log.add("Questions: $amount")
        if (category != null) {
            log.add("Category: ${OpenTDB.categories[category].name}")
        }
        //Dispatch initial tick after 10 seconds
        lastTick = Source.SCHEDULED_EXECUTOR_SERVICE.schedule(
            this::gameTick, 10, TimeUnit.SECONDS
        )
        return TriviaStartResponse()
    }

    class TriviaStartResponse : StandardInfoResponse(
        "Trivia Game Starting!",
        "A new game of Trivia will begin in 10 seconds."
    )

    private fun gameTick() {
        if (current == null) {
            //If there are no more questions, do not re-tick
            current = questions.poll() ?: return stop(StopCause.FINISHED)
            updateMessage(
                QuestionResponse(
                    ++currentRound,
                    totalRounds,
                    current!!
                ).asEmbed(message.author)
            ) {
                setMessage(it)
                validEmotes.map(message::addReaction).forEach(RestAction<Void>::queue)
                //Re-tick after each question is posted
                lastTick = Source.SCHEDULED_EXECUTOR_SERVICE.schedule(
                    this::gameTick, 20, TimeUnit.SECONDS
                )
            }
            return
        } else {
            val question = current!!
            log.add("Current Question: \n\t${question.text}")
            log.add(
                "Possible Answers: \n${
                    question.answers.joinToString(
                        separator = "\n",
                        transform = { "\t${it.text}" })
                }"
            )
            log.add("Correct Answer: ${question.correct}")
            var firstBonus = true
            log.add("Player Answers (User, Answer, Previous Score, New Score, First Correct?): ")
            answers.forEach { (user, answer) ->
                var logLine = "\t${Source.SHARD_MANAGER.getUserById(user)!!.formatLong()}: "
                val score = scores.computeIfAbsent(user) { 0 }
                val choice = question.answers[answer]
                logLine += "${choice.text}, "
                logLine += "${score}, "
                if (choice.correct) {
                    val toIncrement = if (firstBonus) {
                        firstBonus = false
                        2
                    } else 1
                    logLine += "${score + toIncrement}, "
                    logLine += (if (toIncrement == 2) "true" else "false")
                    scores[user] = score + toIncrement
                }
                log.add(logLine)
            }
            answers.clear()
            updateMessage(
                PostRoundResponse(
                    currentRound,
                    totalRounds,
                    question,
                    getTopFive(),
                    questions.size > 0
                ).asEmbed(message.author)
            )
            current = null
            lastTick = Source.SCHEDULED_EXECUTOR_SERVICE.schedule(
                this::gameTick, 5, TimeUnit.SECONDS
            )
        }
    }

    fun stop(cause: StopCause) {
        updateMessage(ScoreResponse(getTopFive()).asEmbed(message.author))
        lastTick.cancel(true)
        log.add("Trivia ${cause.name.lowercase().capitalize()}")
        if (scores.isNotEmpty()) {
            log.add("Final Scores:")
            scores.entries.sortedByDescending { (_, v) -> v }.forEach {
                log.add("\t${Source.SHARD_MANAGER.getUserById(it.key)!!.formatLong()}: ${it.value}")
            }
        }
        uploadLog()
        postGame()
    }

    private fun updateMessage(newContent: MessageEmbed, post: ((Message) -> Unit)? = null) {
        if (post != null) {
            message.delete().queue { triviaListener.unlink(message.id) }
            message.channel.sendMessageEmbeds(newContent).queue(post)
        } else {
            message.delete().complete()
            triviaListener.unlink(message.id)
            setMessage(message.channel.sendMessageEmbeds(newContent).complete())
        }
    }

    private fun getTopFive() = scores.entries.sortedByDescending { (_, v) ->
        v
    }.take(5)

    private fun uploadLog() {
        log.joinToString("\n").byteInputStream().use {
            message.channel.sendFile(it, "trivia.txt").queue()
        }
    }

    class PostRoundResponse(
        round: Int,
        totalRounds: Int,
        question: OpenTDB.Question,
        topFive: List<Map.Entry<String, Int>>,
        hasNext: Boolean
    ) : StandardInfoResponse(
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
    ) : StandardSuccessResponse(
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
    ) : StandardEmbedResponse(
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

    enum class StopCause {
        ABORTED, FINISHED
    }
}