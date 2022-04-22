package net.sourcebot.module.freegames.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.freegames.FreeGames

class RefreshGamesCommand : RootCommand() {
    override val name = "refreshgames"
    override val description = "Checks for new free games and removes expired ones"
    override val guildOnly = true
    override val permission = "freegames.refresh"

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val freeGameHandler = FreeGames.getFreeGameHandler(sender.guild)
        return when (freeGameHandler.refreshGames()) {
            1 -> StandardSuccessResponse("Success!", "Successfully refreshed the free game listings!")
            0 -> StandardInfoResponse("No New Updates!", "There are no new free games or expired listings!")
            else -> StandardErrorResponse("Uh Oh!", "Failed to refresh the free game listings!")
        }

    }
}
