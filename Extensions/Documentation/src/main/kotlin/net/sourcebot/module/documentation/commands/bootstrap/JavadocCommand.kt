package net.sourcebot.module.documentation.commands.bootstrap

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.Source
import net.sourcebot.api.menus.MenuResponse
import net.sourcebot.api.response.Response

abstract class JavadocCommand(
    name: String, description: String
) : DocumentationCommand(name, description) {
    private val menuHandler = Source.MENU_HANDLER
    final override fun postResponse(response: Response, forWhom: User, message: Message) {
        if (response is MenuResponse) menuHandler.link(message, response.menu)
    }
}