package net.sourcebot.impl.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Synopsis
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.SourceAdapter

class PollCommand : RootCommand() {
    override val description = "Create a poll. Poll options are in the format"
    override val name = "poll"

    override val synopsis = Synopsis {
        optParam("channel", "The channel to create the poll in", SourceAdapter.textChannel())
        reqParam("prompt", "The prompt or question for the poll", Adapter.single())
        optParam(
            "options", "The options for the poll, empty for yes/no.", SourceAdapter.dictionary(
                "", ","
            )
        )
    }
}