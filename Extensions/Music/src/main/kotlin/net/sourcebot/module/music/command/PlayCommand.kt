package net.sourcebot.module.music.command

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Argument
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.error.ExceptionResponse
import net.sourcebot.module.music.Music

class PlayCommand : MusicCommand(
    "play", "Play audio from YouTube."
) {
    override val argumentInfo = ArgumentInfo(
        Argument("identifier", "Track identifier to play.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val identifier = args.next("You did not specify a track identifier!")
        val guild = message.guild
        val subsystem = Music.getSubsystem(guild)
        val response = subsystem.scheduler.play(identifier, {
            when (it) {
                is AudioTrack -> InfoResponse("Track Loaded")
                is AudioPlaylist -> InfoResponse("Playlist Loaded")
                else -> EmptyResponse()
            }
        }, {
            InfoResponse("No Match!")
        }, {
            ExceptionResponse(it)
        })
        guild.audioManager.openAudioConnection(message.member?.voiceState?.channel!!)
        return response
    }
}