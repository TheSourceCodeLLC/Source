package net.sourcebot.module.documentation.utility

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.SourceColor

class DocAlert : EmbedBuilder(), Alert {
    override fun asMessage(user: User): Message {
        setColor(SourceColor.INFO.color)
        setFooter("Ran By: ${user.asTag}", user.effectiveAvatarUrl)
        return MessageBuilder(build()).build()
    }

    @JsonSetter("author")
    fun setAuthor(
        node: JsonNode
    ) {
        val name = node["name"]?.asText()
        val url = node["url"]?.asText()
        val iconUrl = node["iconUrl"]?.asText()
        setAuthor(name, url, iconUrl)
    }

    @JsonSetter("url")
    fun setUrl(
        url: String
    ) {
        val title = EmbedBuilder::class.java.getDeclaredField("title").let {
            it.trySetAccessible()
            it[this] as String?
        }
        setTitle(title, url)
    }

    @JsonSetter("fields")
    fun setFields(
        fields: ArrayNode
    ) {
        fields.forEach {
            val name = it["name"].asText()
            val value = it["value"].asText()
            val inline = it["inline"]?.asBoolean()
            addField(name, value, inline ?: false)
        }
    }
}