package net.sourcebot.api.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class AudioSender(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer = ByteBuffer.wrap(lastFrame!!.data)
    override fun isOpus() = true
}