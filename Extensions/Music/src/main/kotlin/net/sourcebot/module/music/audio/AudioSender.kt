package net.sourcebot.module.music.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class AudioSender(
    private val player: AudioPlayer
) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null
    override fun canProvide(): Boolean {
        lastFrame = player.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer = ByteBuffer.wrap(lastFrame!!.data)
    override fun isOpus() = true
}