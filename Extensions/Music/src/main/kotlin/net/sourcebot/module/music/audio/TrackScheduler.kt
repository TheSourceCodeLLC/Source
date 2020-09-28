package net.sourcebot.module.music.audio

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.sourcebot.api.response.Response
import net.sourcebot.module.music.Music
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(
    private val player: AudioPlayer
) : AudioEventAdapter() {
    private val queue = LinkedBlockingQueue<AudioTrack>()

    init {
        player.addListener(this)
        player.volume = 50
    }

    fun offer(track: AudioTrack) {
        if (!player.startTrack(track, true)) queue.offer(track)
    }

    fun skip() = player.startTrack(queue.poll(), false)

    fun stop() {
        queue.clear()
        player.stopTrack()
    }

    fun pause(): Boolean {
        return if (player.isPaused) false
        else {
            player.isPaused = true; true
        }
    }

    fun resume(): Boolean {
        return if (!player.isPaused) false
        else {
            player.isPaused = false; true
        }
    }

    fun play(
        identifier: String,
        onLoad: (AudioItem) -> Response,
        noMatch: () -> Response,
        onFail: (FriendlyException) -> Response
    ): Response {
        val future = CompletableFuture<Response>()
        Music.PLAYER_MANAGER.loadItem(identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                offer(track)
                future.complete(onLoad(track))
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                playlist.tracks.forEach(this@TrackScheduler::offer)
                future.complete(onLoad(playlist))
            }

            override fun noMatches() {
                future.complete(noMatch())
            }

            override fun loadFailed(exception: FriendlyException) {
                future.complete(onFail(exception))
            }
        })
        return future.get()
    }

    override fun onTrackEnd(
        player: AudioPlayer,
        track: AudioTrack,
        endReason: AudioTrackEndReason
    ) {
        if (!endReason.mayStartNext) Unit else skip()
    }
}