package net.sourcebot.module.freegames.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.freegames.FreeGames

class RefreshGamesCommand : RootCommand() {
    override val name = "refreshgames"
    override val description = "Checks for new free games and removes expired ones"
    override val guildOnly = true
    override val permission = "freegames.refresh"

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val user = sender.author
        val channel = sender.textChannel
        val response = StandardInfoResponse(
            "Checking For Games",
            "Started checking for new free game listings! This make take a few seconds."
        )

        channel.sendMessageEmbeds(response.asEmbed(user)).queue { msg ->
            FreeGames.gameEmitter.refreshGuild(sender.guild) {
                channel.editMessageEmbedsById(msg.id, it.asEmbed(user)).queue()
            }
        }

        return EmptyResponse()
    }

    override fun postResponse(response: Response, forWhom: User, message: Message) {

    }
}
