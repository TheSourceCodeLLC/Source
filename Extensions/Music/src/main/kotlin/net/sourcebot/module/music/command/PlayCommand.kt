package net.sourcebot.module.music.command

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.music.Music

class PlayCommand : MusicCommand(
    "play", "Play audio from YouTube."
) {
    override val synopsis = Synopsis {
        reqParam("identifier", "Track identifier to play.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val identifier = arguments.required<String>("identifier", "You did not specify a track identifier!")
        val guild = sender.guild
        val subsystem = Music.getSubsystem(guild)
        val response = subsystem.load(identifier, {
            when (it) {
                is AudioTrack -> StandardInfoResponse("Track Loaded")
                is AudioPlaylist -> StandardInfoResponse("Playlist Loaded")
                else -> EmptyResponse()
            }
        }, { StandardInfoResponse("No Match!") })
        subsystem.connect(sender.member?.voiceState?.channel!!)
        return response
    }
}