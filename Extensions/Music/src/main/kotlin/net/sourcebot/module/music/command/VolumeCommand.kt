package net.sourcebot.module.music.command

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.music.Music

class VolumeCommand : MusicCommand(
    "volume", "View or set the player volume."
) {
    override val synopsis = Synopsis {
        optParam("volume", "The new volume to set.", Adapter.int())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val guild = sender.guild
        val subsystem = Music.getSubsystem(guild)
        val volume = arguments.optional<Int>("volume")
        return if (volume != null) {
            subsystem.player.volume = volume
            StandardSuccessResponse("Volume Set!", "The volume is now `$volume%`!")
        } else {
            StandardSuccessResponse("Current Volume", "The current volume is `${subsystem.player.volume}%`!")
        }
    }
}