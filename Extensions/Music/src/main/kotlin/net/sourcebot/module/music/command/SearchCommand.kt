package net.sourcebot.module.music.command

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.music.Music
import net.sourcebot.module.music.youtube.SearchListItem

class SearchCommand : MusicCommand(
    "search", "Search for audio on YouTube."
) {
    override val cleanupResponse = false
    override fun execute(message: Message, args: Arguments): Response {
        val query = args.slurp(" ", "You did not specify a query!")
        val snippets = Music.YOUTUBE_API.search(query).items.map(SearchListItem::snippet)
        val results = snippets.joinToString("\n") {
            "`${it.title}` by `${it.channelTitle}`"
        }
        return StandardInfoResponse(
            "Search Results", results
        )
    }
}