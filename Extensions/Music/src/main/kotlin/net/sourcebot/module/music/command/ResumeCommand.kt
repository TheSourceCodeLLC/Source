package net.sourcebot.module.music.command

import me.hwiggy.kommander.arguments.Arguments
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.music.Music

class ResumeCommand : MusicCommand(
    "resume", "Resume audio playback."
) {
    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val guild = sender.guild
        val subsystem = Music.getSubsystem(guild)
        return if (subsystem.resume()) {
            StandardSuccessResponse("Playback Resumed!")
        } else {
            StandardErrorResponse(
                "Resume Failure!",
                "The player is not paused!"
            )
        }
    }
}