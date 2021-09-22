package net.sourcebot.module.music.command

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.Source
import net.sourcebot.api.menus.MenuResponse
import net.sourcebot.api.response.EmptyResponse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.music.Music
import net.sourcebot.module.music.youtube.SearchListItem

class SearchCommand : MusicCommand(
    "search", "Search for audio on YouTube."
) {
    private val menuHandler = Source.MENU_HANDLER
    override val cleanupResponse = false
    override val synopsis = Synopsis {
        reqParam("query", "The audio to query for.", Adapter.slurp(" "))
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val query = arguments.required<String>("query", "You did not specify a query!")
        val snippets = Music.YOUTUBE_API.search(query).items.map(SearchListItem::snippet)
        val subsystem = Music.getSubsystem(sender.guild)
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
                        subsystem.connect(sender.member?.voiceState?.channel!!)
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