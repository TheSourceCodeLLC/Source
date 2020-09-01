package net.sourcebot.module.trivia.data

import com.google.gson.Gson
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.sourcebot.Source
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.trivia.Trivia
import org.apache.commons.text.StringEscapeUtils

import org.jsoup.Jsoup
import java.util.*
import java.util.concurrent.TimeUnit

class TriviaGame(
    val gameOptions: GameOptions,
    val channel: MessageChannel,
    val source: Source,
    val trivia: Trivia,
    val gameManager: TriviaGameManager
) {
    private lateinit var questionMessage: Message
    private var questions: Queue<Question> = LinkedList()
    private val scores = hashMapOf<User, Int>()
    private var questionCount = 0
    private var currentQuestion: Question? = null
    private val currentAnswers = hashMapOf<User, Int>()

    fun setup() {
        fetchQuestions()
        questions.forEach {
            it.generateResponseList()
        }
        start()
    }

    private fun start() {
        channel.sendMessage("This Rounds Question").queue {
            this.questionMessage = it
            nextQuestion()
        }

        source.jdaEventSystem.listen(trivia, MessageReactionAddEvent::class.java) {
            if (it.messageId == questionMessage.id && !it.user!!.isBot) {
                currentAnswers[it.user!!] = when (it.reactionEmote.emoji) {

                    "\uD83C\uDDE6" -> 0
                    "\uD83C\uDDE7" -> 1
                    "\uD83C\uDDE8" -> 2
                    "\uD83C\uDDE9" -> 3
                    else -> 99

                }
                it.reaction.removeReaction().complete()
            }
        }

    }

    private fun nextQuestion() {
        questionCount++
        currentQuestion = questions.poll()
        currentAnswers.clear()
        if (currentQuestion == null) {
            finishGame()
            return
        }
        this.questionMessage.editMessage(QuestionResponse().asEmbed(channel.jda.selfUser)).queue {
            it.clearReactions().queue()
            it.addReaction("\uD83C\uDDE6").queue()
            it.addReaction("\uD83C\uDDE7").queue()
            it.addReaction("\uD83C\uDDE8").queue()
            it.addReaction("\uD83C\uDDE9").queue()
        }
        Trivia.scheduledExecutorService.schedule(Runnable {
            evaluateAnswers()
            nextQuestion()
        }, trivia.config.required("question change time"), TimeUnit.SECONDS)

    }

    private fun evaluateAnswers() {
        currentAnswers.forEach { (user, answer) ->
            if (currentQuestion!!.answers[answer].isCorrect) {
                scores[user] = scores[user]?.plus(5) ?: 5
            }
        }
    }

    private fun finishGame() {
        questionMessage.delete().queue()
        channel.sendMessage(
            ScoreResponse(scores.toList().sortedBy { (_, score) -> score }.take(5).toMap()).asEmbed(
                channel.jda.selfUser
            )
        ).queue()
        gameManager.finishGame(questionMessage.guild.idLong)
    }


    private fun fetchQuestions() {
        val json =
            Jsoup.connect("https://opentdb.com/api.php?amount=${gameOptions.amount}&category=${gameOptions.category.id}&difficulty=${gameOptions.difficulty}&type=multiple")
                .ignoreContentType(true).execute().body()
        val response = Gson().fromJson(json, QuestionFetchResponse::class.java)
        response.results.forEach {
            questions.add(
                Question(
                    StringEscapeUtils.UNESCAPE_HTML4.translate(it.question),
                    StringEscapeUtils.unescapeHtml4(it.correct_answer),
                    it.incorrect_answers.map { answer -> StringEscapeUtils.unescapeHtml4(answer) }.toList()
                )
            )
        }
    }


    private inner class QuestionResponse : InfoResponse("Next Question", currentQuestion?.question) {
        init {
            addField("", "\uD83C\uDDE6 **${currentQuestion!!.answers.component1().answer}**", false)
            addField("", "\uD83C\uDDE7 **${currentQuestion!!.answers.component1().answer}**", false)
            addField("", "\uD83C\uDDE8 **${currentQuestion!!.answers.component1().answer}**", false)
            addField("", "\uD83C\uDDE9 **${currentQuestion!!.answers.component1().answer}**", false)
        }
    }

    private inner class ScoreResponse(scores: Map<User, Int>) : SuccessResponse("The game has finished!", "Here are the winners!") {
        init {
            scores.forEach { (user, score) ->
                addField(user.name, score.toString(), false)
            }
        }
    }

}

class QuestionFetchResponse(val response_code: Int, val results: List<Question>)