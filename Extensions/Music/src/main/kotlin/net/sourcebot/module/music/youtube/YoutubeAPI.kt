package net.sourcebot.module.music.youtube

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.urlEncoded
import java.time.format.DateTimeFormatter

class YoutubeAPI(apiKey: String) {
    private val baseUrl = "https://www.googleapis.com/youtube/v3/search"
    private val params = "part=snippet&type=video%2Cplaylist&key=$apiKey"

    fun search(query: String): SearchListResponse =
        JsonSerial.fromUrl("$baseUrl?$params&q=${query.urlEncoded()}")
}

class SearchListResponse @JsonCreator constructor(
    node: ObjectNode
) {
    val kind = node["kind"].asText()
    val etag = node["etag"].asText()
    val nextPageToken = node["nextPageToken"].asText()
    val regionCode = node["regionCode"].asText()
    val pageInfoTotalResults = node["pageInfo"]["totalResults"].asInt()
    val pageInfoResultsPerPage = node["pageInfo"]["resultsPerPage"].asInt()
    val items: Array<SearchListItem> = JsonSerial.fromJson(node["items"])
}

class SearchListItem @JsonCreator constructor(
    node: ObjectNode
) {
    val kind = node["kind"].asText()
    val etag = node["etag"].asText()
    val idKind = node["id"]["kind"].asText()
    val id = when (idKind) {
        "youtube#video" -> node["id"]["videoId"]
        "youtube#channel" -> node["id"]["channelId"]
        "youtube#playlist" -> node["id"]["playlistId"]
        else -> throw IllegalArgumentException("Misunderstood item ID type from Search query!")
    }
    val snippet: Snippet = JsonSerial.fromJson(node["snippet"])
}

class Snippet @JsonCreator constructor(
    node: ObjectNode
) {
    val publishedAt = node["publishedAt"].asText().let {
        DateTimeFormatter.ISO_INSTANT.parse(it)
    }
    val channelId = node["channelId"].asText()
    val title = node["title"].asText()
    val description = node["description"].asText()
    val thumbnails: Map<String, Thumbnail> = JsonSerial.fromJson(node["thumbnails"])
    val channelTitle = node["channelTitle"].asText()
    val liveBroadcastContent = node["liveBroadcastContent"].asText()
}

class Thumbnail @JsonCreator constructor(
    node: ObjectNode
) {
    val url = node["url"].asText()
    val width = node["width"].asInt()
    val height = node["height"].asInt()
}