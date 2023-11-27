package net.sourcebot.module.roleselector.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response

class CreateMenu : RootCommand() {
    override val name: String = "createmenu"
    override val description: String = "Creates a role selector menu."
    override val synopsis: Synopsis = Synopsis {
        reqParam("Menu Name", "", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val channel = sender.channel
        val menu = SelectionMenu.create("test:test")
            .setPlaceholder("I dont know how the fuck this works")
            .setRequiredRange(1, 1)
            .addOption("How the fuck does this work?????", "ok?")
            .build()
        val builder = MessageBuilder().setActionRows(ActionRow.of(menu)).setContent("fuck off")
        channel.sendMessage(builder.build()).queue()
        return EmptyResponse()
    }
}