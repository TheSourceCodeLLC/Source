package net.sourcebot.api.response

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.time.Instant

/**
 * A [Response] represents a [Message] reply to some action by a specific [User]
 */
fun interface Response {
    fun asMessage(user: User): Message
}

/**
 * A type of control [Response] for when a command has no output.
 */
class EmptyResponse : Response {
    override fun asMessage(user: User): Message = throw UnsupportedOperationException()
}

/**
 * Represents a [Response] that renders as a [MessageEmbed]
 */
abstract class SimpleEmbedResponse : Response, EmbedBuilder() {
    final override fun asMessage(
        user: User
    ): Message = MessageBuilder(asEmbed(user)).build()

    open fun asEmbed(user: User): MessageEmbed = super.build()
}

/**
 * Represents a [SimpleEmbedResponse] with standard information applied.
 * Resultant embeds may be personalized for a [User] by rendering their profile photo as the author thumbnail.
 * Alerts also have a timestamp and footer.
 */
abstract class StandardEmbedResponse @JvmOverloads constructor(
    protected var title: String? = null,
    protected var description: String? = null
) : SimpleEmbedResponse() {
    companion object {
        @JvmStatic
        var footer: String? = null
    }

    init {
        setDescription(description)
    }

    override fun asEmbed(user: User): MessageEmbed {
        setAuthor(title ?: String.format("%#s", user), null, user.effectiveAvatarUrl)
        setTimestamp(Instant.now())
        setFooter(footer)
        return super.build()
    }

    @Throws(UnsupportedOperationException::class)
    override fun build() = throw UnsupportedOperationException("Responses may not be built raw!")
}

/**
 * Represents an [SimpleEmbedResponse] with a given [Color]
 */
abstract class SimpleColoredResponse(
    color: Color
) : SimpleEmbedResponse() {
    constructor(color: SourceColor) : this(color.color)

    init {
        setColor(color)
    }
}

/**
 * Represents a [StandardEmbedResponse] with a given [Color].
 */
abstract class StandardColoredResponse @JvmOverloads constructor(
    title: String? = null,
    description: String? = null,
    color: Color
) : StandardEmbedResponse(title, description) {

    @JvmOverloads
    constructor(
        title: String? = null,
        description: String? = null,
        sourceColor: SourceColor
    ) : this(title, description, sourceColor.color)

    init {
        setColor(color)
    }
}

/**
 * Represents a [SimpleColoredResponse] using the color [SourceColor.INFO]
 */
open class SimpleInfoResponse : SimpleColoredResponse(SourceColor.INFO)

/**
 * Represents a [SimpleColoredResponse] using the color [SourceColor.SUCCESS]
 */
open class SimpleSuccessResponse : SimpleColoredResponse(SourceColor.SUCCESS)

/**
 * Represents a [SimpleColoredResponse] using the color [SourceColor.WARNING]
 */
open class SimpleWarningResponse : SimpleColoredResponse(SourceColor.WARNING)

/**
 * Represents a [SimpleColoredResponse] using the color [SourceColor.ERROR]
 */
open class SimpleErrorResponse : SimpleColoredResponse(SourceColor.ERROR)

/**
 * Represents a [StandardColoredResponse] using the color [SourceColor.INFO]
 */
open class StandardInfoResponse @JvmOverloads constructor(
    title: String? = null,
    description: String? = null
) : StandardColoredResponse(title, description, SourceColor.INFO)

/**
 * Represents a [StandardColoredResponse] using the color [SourceColor.SUCCESS]
 */
open class StandardSuccessResponse @JvmOverloads constructor(
    title: String? = null,
    description: String? = null
) : StandardColoredResponse(title, description, SourceColor.SUCCESS)

/**
 * Represents a [StandardColoredResponse] using the color [SourceColor.WARNING]
 */
open class StandardWarningResponse @JvmOverloads constructor(
    title: String? = null,
    description: String? = null
) : StandardColoredResponse(title, description, SourceColor.WARNING)

/**
 * Represents a [StandardColoredResponse] using the color [SourceColor.ERROR]
 */
open class StandardErrorResponse @JvmOverloads constructor(
    title: String? = null,
    description: String? = null
) : StandardColoredResponse(title, description, SourceColor.ERROR)