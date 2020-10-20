package net.sourcebot.module.applications.data

import net.dv8tion.jda.api.entities.User

data class ActiveApplicationModel(
    val user: User,
    val guildId: String,
    val answerMap: MutableMap<Int, String>,
    val appModel: ApplicationModel
) {

    fun addAnswer(questionNumber: Int, answer: String) {
        answerMap[questionNumber] = answer
    }

}