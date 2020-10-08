package net.sourcebot.module.music.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Adapter
import net.sourcebot.api.command.argument.ArgumentInfo
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.command.argument.OptionalArgument
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardSuccessResponse
import net.sourcebot.module.music.Music

class VolumeCommand : MusicCommand(
    "volume", "View or set the player volume."
) {
    override val argumentInfo = ArgumentInfo(
        OptionalArgument("volume", "The new volume to set.")
    )

    override fun execute(message: Message, args: Arguments): Response {
        val guild = message.guild
        val subsystem = Music.getSubsystem(guild)
        val volume = args.next(Adapter.int())
        return if (volume != null) {
            subsystem.player.volume = volume
            StandardSuccessResponse("Volume Set!", "The volume is now `$volume%`!")
        } else {
            StandardSuccessResponse("Current Volume", "The current volume is `${subsystem.player.volume}%`!")
        }
    }
}