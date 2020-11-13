package net.sourcebot.module.music.youtube

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.node.ObjectNode
import net.sourcebot.api.configuration.JsonConfiguration
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.urlEncoded
import java.time.format.DateTimeFormatter

class YoutubeAPI(apiKey: String) {
    private val baseUrl = "https://www.googleapis.com/youtube/v3/search"
    private val params = "part=snippet&type=video%2Cplaylist&key=$apiKey"

    fun search(query: String): SearchListResponse =
        JsonSerial.fromUrl("$baseUrl?$params&q=${query.urlEncoded()}")
}

class SearchListResponse @JsonCreator constructor(node: ObjectNode) : JsonConfiguration(node) {
    val kind: String by delegateRequired()
    val etag: String by delegateRequired()
    val nextPageToken: String by delegateRequired()
    val regionCode: String by delegateRequired()
    val pageInfoTotalResults: Int by delegateRequired("pageInfo.totalResults")
    val pageInfoResultsPerPage: Int by delegateRequired("pageInfo.resultsPerPage")
    val items: Array<SearchListItem> by delegateRequired()
}

class SearchListItem @JsonCreator constructor(node: ObjectNode) : JsonConfiguration(node) {
    val kind: String by delegateRequired()
    val etag: String by delegateRequired()
    val idKind: String by delegateRequired("id.kind")
    val id: String = when (idKind) {
        "youtube#video" -> required("id.videoId")
        "youtube#channel" -> required("id.channelId")
        "youtube#playlist" -> required("id.playlistId")
        else -> throw IllegalArgumentException("Misunderstood item ID type from Search query!")
    }
    val snippet = Snippet(id, required("snippet"))
}

class Snippet @JsonCreator constructor(
    id: String,
    node: ObjectNode
) : JsonConfiguration(node) {
    val publishedAt = node["publishedAt"].asText().let {
        DateTimeFormatter.ISO_INSTANT.parse(it)
    }
    val channelId = node["channelId"].asText()
    val title = node["title"].asText()
    val description = node["description"].asText()
    val thumbnails: Map<String, Thumbnail> = JsonSerial.fromJson(node["thumbnails"])
    val channelTitle = node["channelTitle"].asText()
    val liveBroadcastContent = node["liveBroadcastContent"].asText()
    val url = "https://youtu.be/$id"
}

class Thumbnail @JsonCreator constructor(node: ObjectNode) : JsonConfiguration(node) {
    val url: String by delegateRequired()
    val width: Int by delegateRequired()
    val height: Int by delegateRequired()
}