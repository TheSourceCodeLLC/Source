package net.sourcebot.module.trivia.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.configuration.Properties
import net.sourcebot.api.urlDecoded

object OpenTDB {
    private const val baseUrl = "https://opentdb.com"
    private const val apiUrl = "$baseUrl/api.php"
    private const val categoryUrl = "$baseUrl/api_category.php"

    val categories: List<Category> by lazy {
        JsonSerial.fromUrl<Properties>(categoryUrl).required("trivia_categories")
    }

    private val categoryIds by lazy { categories.map(Category::id) }

    @JvmStatic
    fun requestQuestions(
        amount: Int = 5,
        category: Int? = null,
    ): List<Question> {
        var reqUrl = "$apiUrl?amount=$amount"
        if (category != null) reqUrl += "&category=$category"
        reqUrl += "&type=multiple&encode=url3986"
        val response: Properties = JsonSerial.fromUrl(reqUrl)
        return response.required("results")
    }

    @JvmStatic
    fun isValidCategory(id: Int) = categoryIds.contains(id)

    class Category @JsonCreator constructor(
        @JsonProperty("id") val id: Int,
        @JsonProperty("name") val name: String
    )

    class Question @JsonCreator constructor(
        @JsonProperty("category") category: String,
        @JsonProperty("type") type: String,
        @JsonProperty("difficulty") difficulty: String,
        @JsonProperty("question") question: String,
        @JsonProperty("correct_answer") correct: String,
        @JsonProperty("incorrect_answers") incorrect: Array<String>,
    ) {
        val category: String = category.urlDecoded()
        val type: String = type.urlDecoded().capitalize()
        val difficulty: String = difficulty.urlDecoded().capitalize()
        val text: String = question.urlDecoded()
        val correct: String = correct.urlDecoded()
        val answers = listOf(
            Answer(correct, true),
            *incorrect.map { Answer(it, false) }.toTypedArray()
        ).shuffled()

        class Answer(text: String, val correct: Boolean) {
            val text: String = text.urlDecoded()
        }
    }
}