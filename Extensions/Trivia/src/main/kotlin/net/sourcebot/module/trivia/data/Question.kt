package net.sourcebot.module.trivia.data


data class Question(val question: String, val correct_answer: String, val incorrect_answers: List<String>) {

    lateinit var answers: List<QuestionAnswer>
        private set

    fun generateResponseList() {
        val unshuffeledAnswers = mutableListOf<QuestionAnswer>()
        unshuffeledAnswers.add(QuestionAnswer(correct_answer, true))
        incorrect_answers.forEach {
            unshuffeledAnswers.add(QuestionAnswer(it, false))
        }
        answers = unshuffeledAnswers.shuffled()
    }


    class QuestionAnswer(val answer: String, val isCorrect: Boolean)

}