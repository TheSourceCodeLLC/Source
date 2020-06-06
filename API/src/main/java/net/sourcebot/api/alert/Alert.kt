package net.sourcebot.api.alert

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.time.Instant

/**
 * An Alert represents an embed with some basic information pre-attached.
 * Alerts should be built for a specific [User]
 * Resultant embeds will be personalized for a [User] by rendering their profile photo as the author thumbnail.
 * Alerts also have a timestamp and footer.
 */
abstract class Alert internal constructor(
    protected var title: String,
    protected var description: String
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

/**
 * Represents an Alert with a given color.
 */
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

/**
 * Represents a [ColoredAlert] using the color [SourceColor.INFO]
 */
open class InfoAlert(
    title: String,
    description: String = ""
) : ColoredAlert(title, description, SourceColor.INFO)

/**
 * Represents a [ColoredAlert] using the color [SourceColor.SUCCESS]
 */
open class SuccessAlert(
    title: String,
    description: String = ""
) : ColoredAlert(title, description, SourceColor.SUCCESS)

/**
 * Represents a [ColoredAlert] using the color [SourceColor.WARNING]
 */
open class WarningAlert(
    title: String,
    description: String = ""
) : ColoredAlert(title, description, SourceColor.WARNING)

/**
 * Represents a [ColoredAlert] using the color [SourceColor.ERROR]
 */
open class ErrorAlert(
    title: String,
    description: String = ""
) : ColoredAlert(title, description, SourceColor.ERROR)