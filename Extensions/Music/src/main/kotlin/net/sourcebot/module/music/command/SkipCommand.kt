package net.sourcebot.module.music.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.music.Music

class SkipCommand : MusicCommand(
    "skip", "Skip to the next audio source."
) {
    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val subsystem = Music.getSubsystem(guild)
        val scheduler = subsystem.scheduler
        return if (scheduler.skip()) {
            val track = subsystem.player.playingTrack
            StandardInfoResponse(track.toString())
        } else {
            StandardSuccessResponse("Queue Empty!", "There are no more songs in the queue!")
        }
    }
}