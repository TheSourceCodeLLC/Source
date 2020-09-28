package net.sourcebot.module.music.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.sourcebot.api.response.Response
import net.sourcebot.module.music.Music
import java.util.concurrent.CompletableFuture

class AudioSubsystem(
    val player: AudioPlayer
) {
    private val audioSender = AudioSender(player)
    val scheduler = TrackScheduler(player)

    fun load(
        identifier: String,
        postLoad: (AudioItem) -> Response,
        noMatch: () -> Response,
        onFailed: (FriendlyException) -> Response
    ): Response = CompletableFuture<Response>().apply {
        Music.PLAYER_MANAGER.loadItem(identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                scheduler.offer(track)
                complete(postLoad(track))
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                playlist.tracks.forEach(scheduler::offer)
                complete(postLoad(playlist))
            }

            override fun noMatches() {
                complete(noMatch())
            }

            override fun loadFailed(exception: FriendlyException) {
                complete(onFailed(exception))
            }
        })
    }.get()

    fun applyTo(guild: Guild) {
        guild.audioManager.sendingHandler = audioSender
    }
}