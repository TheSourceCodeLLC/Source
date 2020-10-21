package net.sourcebot.module.documentation.commands.bootstrap

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.menus.MenuHandler
import net.sourcebot.api.menus.MenuResponse
import net.sourcebot.api.response.Response

abstract class JavadocCommand(
    name: String, description: String, private val menuHandler: MenuHandler
) : DocumentationCommand(name, description) {
    final override fun postResponse(response: Response, forWhom: User, message: Message) {
        if (response is MenuResponse) menuHandler.link(message, response.menu)
    }
}