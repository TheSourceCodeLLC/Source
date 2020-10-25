package net.sourcebot.module.music.command

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.menus.MenuHandler
import net.sourcebot.api.menus.MenuResponse
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.music.Music
import net.sourcebot.module.music.youtube.SearchListItem

class SearchCommand(
    private val menuHandler: MenuHandler
) : MusicCommand(
    "search", "Search for audio on YouTube."
) {
    override val cleanupResponse = false
    override fun execute(message: Message, args: Arguments): Response {
        val query = args.slurp(" ", "You did not specify a query!")
        val snippets = Music.YOUTUBE_API.search(query).items.map(SearchListItem::snippet)
        val subsystem = Music.getSubsystem(message.guild)
        return menuHandler.createSelectionMenu(
            snippets, 5, {
                MarkdownUtil.maskedLink(
                    "${it.title} by ${it.channelTitle}", it.url
                )
            }, {
                return@createSelectionMenu subsystem.scheduler.play(it.url, { item ->
                    val initial = when (item) {
                        is AudioTrack -> StandardInfoResponse(
                            "Track Queued"
                        )
                        is AudioPlaylist -> StandardInfoResponse("Playlist Queued")
                        else -> return@play EmptyResponse()
                    }
                    initial.apply {
                        addField("Title", it.title, false)
                        addField("Channel", it.channelTitle, false)
                        setThumbnail(it.thumbnails["default"]!!.url)
                    }.also {
                        subsystem.connect(message.member?.voiceState?.channel!!)
                    }
                }, {
                    StandardErrorResponse("Unknown Video / Playlist!", "")
                })
            }
        ).render()
    }

    override fun postResponse(response: Response, forWhom: User, message: Message) {
        if (response is MenuResponse) menuHandler.link(message, response.menu)
    }
}