package net.sourcebot.module.documentation.commands.bootstrap

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.SelectorModel

abstract class JavadocCommand(
    name: String, description: String
) : DocumentationCommand(name, description) {
    final override fun postResponse(response: Response, forWhom: User, message: Message) {
        SelectorModel.selectorCache.updateSelector(forWhom, message)
    }
}