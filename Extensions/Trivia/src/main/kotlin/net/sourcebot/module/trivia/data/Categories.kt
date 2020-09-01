package net.sourcebot.module.trivia.data

import com.google.gson.Gson
import org.jsoup.Jsoup

data class Categories(val trivia_categories: List<Category>) {

    fun contains(name: String): Boolean {
        return trivia_categories.firstOrNull { it.name.toLowerCase() == name.toLowerCase() } != null
    }

    fun containsID(id: String): Boolean {
        return trivia_categories.firstOrNull { it.id == id.toIntOrNull() ?: 99 } != null
    }

    fun containsID(id: Int): Boolean {
        return trivia_categories.firstOrNull { it.id == id } != null
    }

    fun search(query: String): List<Category> {
        return trivia_categories.filter { it.name.contains(query) }
    }

    companion object {
        fun fetchCategories(): Categories {
            val jsonString =
                Jsoup.connect("https://opentdb.com/api_category.php").ignoreContentType(true).execute().body()
            return Gson().fromJson(jsonString, Categories::class.java)
        }
    }

    data class Category(val id: Int, val name: String)
}