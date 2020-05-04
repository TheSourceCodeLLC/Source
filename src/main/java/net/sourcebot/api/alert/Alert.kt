package net.sourcebot.api.alert

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.misc.SourceColor
import java.awt.Color
import java.time.Instant

abstract class Alert internal constructor(
    private val title: String,
    private val description: String
) : EmbedBuilder() {
    fun buildFor(user: User): MessageEmbed {
        setAuthor(title, null, user.effectiveAvatarUrl)
        setDescription(description)
        setTimestamp(Instant.now())
        setFooter("TheSourceCode • https://sourcebot.net")
        return super.build()
    }

    @Throws(UnsupportedOperationException::class)
    override fun build() =
        throw UnsupportedOperationException("Alerts may not be built raw! Use buildFor instead!")
}

abstract class ColoredAlert internal constructor(
    title: String,
    description: String,
    color: Color
) : Alert(title, description) {

    internal constructor(
        title: String,
        description: String,
        sourceColor: SourceColor
    ) : this(title, description, sourceColor.color)

    init {
        setColor(color)
    }
}

open class InfoAlert(
    title: String,
    description: String
) : ColoredAlert(title, description, SourceColor.INFO)

open class SuccessAlert(
    title: String,
    description: String
) : ColoredAlert(title, description, SourceColor.SUCCESS)

open class WarningAlert(
    title: String,
    description: String
) : ColoredAlert(title, description, SourceColor.WARNING)

open class ErrorAlert(
    title: String,
    description: String
) : ColoredAlert(title, description, SourceColor.ERROR)