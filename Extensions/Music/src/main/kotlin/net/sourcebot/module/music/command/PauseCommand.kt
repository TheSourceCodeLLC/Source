package net.sourcebot.module.music.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.music.Music

class PauseCommand : MusicCommand(
    "pause", "Pause audio playback."
) {
    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val subsystem = Music.getSubsystem(guild)
        return if (subsystem.scheduler.pause()) {
            StandardSuccessResponse("Playback Paused!")
        } else {
            StandardErrorResponse(
                "Pause Failure!",
                "The player is already paused!"
            )
        }
    }
}