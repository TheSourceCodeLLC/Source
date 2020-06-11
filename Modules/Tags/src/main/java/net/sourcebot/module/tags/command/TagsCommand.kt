package net.sourcebot.module.tags.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.alert.SuccessAlert
import net.sourcebot.api.command.Command
import net.sourcebot.api.command.InvalidSyntaxException
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
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
        addChild(TagsCreateCommand())
        addChild(TagsDeleteCommand())
        addChild(TagsEditCommand())
        addChild(TagsInfoCommand())
        addChild(TagsListCommand())
    }

    private inner class TagsCreateCommand : Command() {
        override val name = "create"
        override val description = "Create a tag."
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag you wish to create."),
            Argument("content", "The content of the created tag.")
        )
        override val permission = "tags.create"

        override fun execute(message: Message, args: Arguments): Alert {
            val tagCache = tagHandler.getCache(message.guild)
            val name = args.next("You did not specify a name for the new tag!").toLowerCase()
            val existing = tagCache.getTag(name)
            if (existing != null) return ErrorAlert("Duplicate Tag!", "A tag named `$name` already exists!")
            val content = args.slurp(" ", "You did not specify content for the new tag!")
            tagCache.createTag(name, content, message.author.id)
            return SuccessAlert("Tag Created!", "The tag named `$name` has been created!")
        }
    }

    private inner class TagsDeleteCommand : Command() {
        override val name = "delete"
        override val description = "Delete a tag"
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag you wish to delete.")
        )
        override val permission = "tags.delete"

        override fun execute(message: Message, args: Arguments): Alert {
            val tagCache = tagHandler.getCache(message.guild)
            val name = args.next("You did not specify a tag to delete!").toLowerCase()
            tagCache.getTag(name) ?: return NoSuchTagAlert(name)
            tagCache.deleteTag(name)
            return SuccessAlert("Tag Deleted!", "The tag named `$name` has been deleted!")
        }
    }

    private inner class TagsEditCommand : Command() {
        override val name = "edit"
        override val description = "Edit tag properties."
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag to edit."),
            Argument("category|content|type", "The property of the tag to edit."),
            Argument("value", "The new value of the property.")
        )
        override val permission = "tags.edit"

        override fun execute(message: Message, args: Arguments): Alert {
            val tagCache = tagHandler.getCache(message.guild)
            val name = args.next("You did not specify a tag to edit!").toLowerCase()
            val tag = tagCache.getTag(name) ?: return NoSuchTagAlert(name)
            return when (val property = args.next("You did not specify a property to edit!").toLowerCase()) {
                "category" -> {
                    tag.category = args.next("You did not specify a new tag category!")
                    SuccessAlert("Category Updated!", "The tag `$name` now belongs to category `$${tag.category}`!")
                }
                "content" -> {
                    tag.content = args.slurp(" ", "You did not specify new tag content!")
                    SuccessAlert("Content Updated!", "Successfully changed the content for tag `$name`!")
                }
                "type" -> {
                    when (args.next("You did not specify a new tag type!").toLowerCase()) {
                        "embed" -> tag.type = Tag.Type.EMBED
                        else -> tag.type = Tag.Type.TEXT
                    }
                    SuccessAlert("Type Updated!", "The type of tag `$name` is now: ${tag.type.name.toLowerCase()}!")
                }
                else -> throw InvalidSyntaxException("Invalid property `$property`!")
            }
        }
    }

    private inner class TagsInfoCommand : Command() {
        override val name = "info"
        override val description = "Show tag information."
        override val argumentInfo = ArgumentInfo(
            Argument("name", "The name of the tag you wish to view info for.")
        )

        override fun execute(message: Message, args: Arguments): Alert {
            val tagCache = tagHandler.getCache(message.guild)
            val name = args.next("You did not specify a tag to show info for!").toLowerCase()
            val tag = tagCache.getTag(name) ?: return NoSuchTagAlert(name)
            return TagInfoAlert(tag, message.jda)
        }
    }

    private inner class TagsListCommand : Command() {
        override val name = "list"
        override val description = "List all tags."

        override fun execute(message: Message, args: Arguments): Alert {
            val tagCache = tagHandler.getCache(message.guild)
            val tags = tagCache.getTags()
            if (tags.isEmpty()) return InfoAlert("Tag Listing", "There are currently no tags.")
            return TagListAlert(tags.groupBy { it.category })
        }
    }

    private class NoSuchTagAlert(name: String) : ErrorAlert("Invalid Tag!", "There is no tag named `$name`!")
    private class TagInfoAlert(
        tag: Tag,
        jda: JDA
    ) : InfoAlert("Tag Information", "Information for tag `${tag.name}`:") {
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

    private class TagListAlert(grouped: Map<String, List<Tag>>) : InfoAlert("Tag Listing") {
        init {
            grouped.forEach { (k, v) ->
                val list = v.joinToString(",") { "`${it.name}`" }
                addField(k, list, false)
            }
        }
    }
}