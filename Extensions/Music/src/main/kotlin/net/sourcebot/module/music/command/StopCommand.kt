package net.sourcebot.module.music.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.SuccessResponse
import net.sourcebot.module.music.Music

class StopCommand : MusicCommand(
    "stop", "Stop audio playback."
) {
    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val subsystem = Music.getSubsystem(guild)
        subsystem.scheduler.stop()
        return SuccessResponse("Playback Stopped!")
    }
}