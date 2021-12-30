package net.sourcebot.module.tags.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.Source
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.SourceCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.tags.data.Tag
import net.sourcebot.module.tags.data.TagHandler
import java.time.Instant

class TagsCommand(
    private val tagHandler: TagHandler
) : RootCommand() {
    override val name = "tags"
    override val description = "Manage Tags."
    override val guildOnly = true
    override val aliases = listOf("tag")
    override val permission = name

    init {
        register(
            TagsCreateCommand(),
            TagsDeleteCommand(),
            TagsEditCommand(),
            TagsInfoCommand(),
            TagsListCommand(),
            TagsRawCommand()
        )
    }

    private inner class TagsCreateCommand : Bootstrap(
        "create",
        "Create a tag."
    ) {
        override val synopsis = Synopsis {
            reqParam("name", "The name of the tag to be created.", Adapter.single())
            reqParam("content", "The content of the created tag.", Adapter.slurp(" "))
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagCache = tagHandler[sender.guild]
            val name = arguments.required("name", "You did not specify a name for the new tag!", String::toLowerCase)
            val existing = tagCache.getTag(name)
            if (existing != null) return StandardErrorResponse("Duplicate Tag!", "A tag named `$name` already exists!")
            val content = arguments.required<String>("content", "You did not specify content for the new tag!")
            tagCache.createTag(name, content, sender.author.id)
            return StandardSuccessResponse("Tag Created!", "The tag named `$name` has been created!")
        }
    }

    private inner class TagsDeleteCommand : Bootstrap(
        "delete",
        "Delete a tag."
    ) {
        override val synopsis = Synopsis {
            reqParam("name", "The name of the tag to delete.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagCache = tagHandler[sender.guild]
            val name = arguments.required("name", "You did not specify a tag to delete!", String::toLowerCase)
            tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            tagCache.deleteTag(name)
            return StandardSuccessResponse("Tag Deleted!", "The tag named `$name` has been deleted!")
        }
    }

    private inner class TagsEditCommand : Bootstrap(
        "edit",
        "Edit tag properties."
    ) {
        override val synopsis = Synopsis {
            reqParam("name", "The name of the tag to edit.", Adapter.single())
            reqGroup("property", "The property of the tag to edit.", Group.Option.byName<TagProperty>()) {
                choice(TagProperty.CONTENT, "Modifies the tag content.")
                choice(TagProperty.CATEGORY, "Modifies the tag category.")
                choice(TagProperty.TYPE, "Modifies the tag type.")
            }
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagCache = tagHandler[sender.guild]
            val name = arguments.required<String>("name", "You did not specify a tag to edit!").toLowerCase()
            val tag = tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            val property = arguments.required<TagProperty>("property", "You did not specify a property to edit!")
            val processed = property.synopsis.process(arguments.parent.slice())
            val alert = property.applicator(tag, processed)
            tagCache.saveTag(tag)
            return alert
        }
    }

    private inner class TagsInfoCommand : Bootstrap(
        "info",
        "Show tag information."
    ) {
        override val synopsis = Synopsis {
            reqParam("name", "The name of the tag to view info for.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagCache = tagHandler[sender.guild]
            val name = arguments.required("name", "You did not specify a tag to show info for!", String::toLowerCase)
            val tag = tagCache.getTag(name) ?: return NoSuchTagResponse(name)
            return TagInfoResponse(tag, sender.jda)
        }
    }

    private inner class TagsListCommand : Bootstrap(
        "list",
        "List all tags."
    ) {
        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagCache = tagHandler[sender.guild]
            val tags = tagCache.getTags()
            if (tags.isEmpty()) return StandardInfoResponse("Tag Listing", "There are currently no tags.")
            return TagListResponse(tags.groupBy { it.category })
        }
    }

    private inner class TagsRawCommand : Bootstrap(
        "raw", "Show the raw content of a tag."
    ) {
        override val aliases = listOf("source")
        override val synopsis = Synopsis {
            reqParam("tag", "The name of the tag to view content of.", Adapter.single())
        }

        override fun execute(sender: Message, arguments: Arguments.Processed): Response {
            val tagName =
                arguments.required("tag", "You did not specify a tag to view content of!", String::toLowerCase)
            val tagCache = tagHandler[sender.guild]
            val tag = tagCache.getTag(tagName) ?: return NoSuchTagResponse(tagName)
            val content = tag.content.replace("```", "`\u200b``")
            return Response { MessageBuilder("```markdown\n$content\n```").build() }
        }
    }

    private class NoSuchTagResponse(name: String) :
        StandardErrorResponse("Invalid Tag!", "There is no tag named `$name`!")

    private class TagInfoResponse(tag: Tag, jda: JDA) : StandardInfoResponse(
        "Tag Information", "Information for tag `${tag.name}`:"
    ) {
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

    private class TagListResponse(grouped: Map<String, List<Tag>>) : StandardInfoResponse("Tag Listing") {
        init {
            grouped.forEach { (k, v) ->
                val list = v.joinToString(",") { "`${it.name}`" }
                addField(k, list, false)
            }
        }
    }

    private abstract class Bootstrap(
        final override val name: String,
        final override val description: String
    ) : SourceCommand() {
        final override val permission = "tags.$name"
        final override val guildOnly = true
    }
}

enum class TagProperty(
    override val synopsisName: String,
    val synopsis: Synopsis,
    val applicator: (Tag, Arguments.Processed) -> Response
) : Group.Option {
    CATEGORY("category", Synopsis {
        reqParam("category", "The new category for this tag.", Adapter.single())
    }, { tag, args ->
        tag.category = args.required("category", "You did not specify a new tag category!")
        StandardSuccessResponse(
            "Category Updated!",
            "The tag `${tag.name}` now belongs to category `${tag.category}`!"
        )
    }),
    CONTENT("content", Synopsis {
        reqParam("content", "The new content for this tag.", Adapter.slurp(" "))
    }, { tag, args ->
        tag.content = args.required("content", "You did not specify new tag content!")
        StandardSuccessResponse(
            "Content Updated!",
            "Successfully changed the content for tag `${tag.name}`!"
        )
    }),
    TYPE("type", Synopsis {
        reqGroup("type", "The new type for this tag.", Group.Option.byName<Tag.Type>()) {
            choice(Tag.Type.EMBED, "Renders the tag inside an embed.")
            choice(Tag.Type.TEXT, "Renders the tag as plain text.")
        }
    }, { tag, args ->
        tag.type = args.required<Tag.Type>("type", "You did not specify a new tag type!")
        StandardSuccessResponse(
            "Type Updated!",
            "The type of tag `${tag.name}` is now: ${tag.type.name.toLowerCase()}!"
        )
    })
}