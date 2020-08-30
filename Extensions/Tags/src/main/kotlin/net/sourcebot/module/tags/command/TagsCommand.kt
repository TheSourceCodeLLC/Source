package net.sourcebot.module.tags.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.tags.data.Tag
import net.sourcebot.module.tags.data.TagHandler
import java.time.Instant

class TagsCommand(
    private val tagHandler: TagHandler
) : RootCommand() {
    override val name = "tags"
    override val description = "Manage Tags."
    override val guildOnly = true
    override val aliases = arrayOf("tag")

    init {
        addChildren(
            TagsCreateCommand(),
            TagsDeleteCommand(),
            TagsEditCommand(),
            TagsInfoCommand(),
            TagsListCommand(),
            TagsRawCommand()
        )
    }

    private inner class TagsCreateCommand : CommandBootstrap(
        "create",
        "Create a tag."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the created tag."),
            Argument("content", "The content of the created tag.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val tagCache = tagHandler[message.guild]
            val name = args.next("You did not specify a name for the new tag!").toLowerCase()
            val existing = tagCache.getTag(name)
            if (existing != null) return ErrorResponse("Duplicate Tag!", "A tag named `$name` already exists!")
            val content = args.slurp(" ", "You did not specify content for the new tag!")
            tagCache.createTag(name, content, message.author.id)
            return SuccessResponse("Tag Created!", "The tag named `$name` has been created!")
        }
    }

    private inner class TagsDeleteCommand : CommandBootstrap(
        "delete",
        "Delete a tag."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag to delete.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val tagCache = tagHandler[message.guild]
            val name = args.next("You did not specify a tag to delete!").toLowerCase()
            tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            tagCache.deleteTag(name)
            return SuccessResponse("Tag Deleted!", "The tag named `$name` has been deleted!")
        }
    }

    private inner class TagsEditCommand : CommandBootstrap(
        "edit",
        "Edit tag properties."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag to edit."),
            Argument("category|content|type", "The property of the tag to edit."),
            Argument("value", "The new value of the property.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val tagCache = tagHandler[message.guild]
            val name = args.next("You did not specify a tag to edit!").toLowerCase()
            val tag = tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            val alert = when (val property = args.next("You did not specify a property to edit!").toLowerCase()) {
                "category" -> {
                    tag.category = args.next("You did not specify a new tag category!")
                    SuccessResponse("Category Updated!", "The tag `$name` now belongs to category `${tag.category}`!")
                }
                "content" -> {
                    tag.content = args.slurp(" ", "You did not specify new tag content!")
                    SuccessResponse("Content Updated!", "Successfully changed the content for tag `$name`!")
                }
                "type" -> {
                    when (args.next("You did not specify a new tag type!").toLowerCase()) {
                        "embed" -> tag.type = Tag.Type.EMBED
                        else -> tag.type = Tag.Type.TEXT
                    }
                    SuccessResponse("Type Updated!", "The type of tag `$name` is now: ${tag.type.name.toLowerCase()}!")
                }
                else -> throw InvalidSyntaxException("Invalid property `$property`!")
            }
            tagCache.saveTag(tag)
            return alert
        }
    }

    private inner class TagsInfoCommand : CommandBootstrap(
        "info",
        "Show tag information."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag to view info for.")
        )

        override fun execute(message: Message, args: Arguments): Response {
            val tagCache = tagHandler[message.guild]
            val name = args.next("You did not specify a tag to show info for!").toLowerCase()
            val tag = tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            return TagInfoResponse(tag, message.jda)
        }
    }

    private inner class TagsListCommand : CommandBootstrap(
        "list",
        "List all tags."
    ) {
        override fun execute(message: Message, args: Arguments): Response {
            val tagCache = tagHandler[message.guild]
            val tags = tagCache.getTags()
            if (tags.isEmpty()) return InfoResponse("Tag Listing", "There are currently no tags.")
            return TagListResponse(tags.groupBy { it.category })
        }
    }

    private inner class TagsRawCommand : CommandBootstrap(
        "raw", "Show the raw content of a tag."
    ) {
        override val argumentInfo = ArgumentInfo(
            Argument("tag", "The name of the tag to view content of.")
        )
        override val aliases = arrayOf("source")

        override fun execute(message: Message, args: Arguments): Response {
            val tagName = args.next("You did not specify a tag to view content of!").toLowerCase()
            val tagCache = tagHandler[message.guild]
            val tag = tagCache.getTag(tagName) ?: return NoSuchTagResponse(tagName)
            val content = tag.content.replace("```", "`\u200b``")
            return Response { MessageBuilder("```markdown\n$content\n```").build() }
        }
    }

    private class NoSuchTagResponse(name: String) : ErrorResponse("Invalid Tag!", "There is no tag named `$name`!")
    private class TagInfoResponse(
        tag: Tag,
        jda: JDA
    ) : InfoResponse("Tag Information", "Information for tag `${tag.name}`:") {
        init {
            val creatorUser = jda.getUserById(tag.creator)
            val creator = if (creatorUser == null) "Unknown" else String.format("%#s", creatorUser)
            addField("Creator", creator, false)
            val created = Instant.ofEpochMilli(tag.created).atZone(Source.TIME_ZONE)
            val formatted = Source.DATE_TIME_FORMAT.format(created)
            addField("Created", formatted, false)
            addField("Category", tag.category, false)
            addField("Uses", tag.uses.toString(), false)
        }
    }

    private class TagListResponse(grouped: Map<String, List<Tag>>) : InfoResponse("Tag Listing") {
        init {
            grouped.forEach { (k, v) ->
                val list = v.joinToString(",") { "`${it.name}`" }
                addField(k, list, false)
            }
        }
    }

    private abstract class CommandBootstrap(
        final override val name: String,
        final override val description: String
    ) : Command() {
        final override val permission by lazy { "tags.$name" }
        final override val guildOnly = true
    }
}