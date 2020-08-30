package net.sourcebot.module.documentation.commands

import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.DocResponse
import org.jsoup.Connection
import org.jsoup.Jsoup

class DJSCommand : RootCommand() {
    override val name: String = "djs"
    override val description: String = "Allows the user to query the Discord.JS Documentation."
    override var cleanupResponse: Boolean = false

    private val defaultSources: MutableList<String> = mutableListOf(
        "stable", "master", "rpc", "commando", "akairo", "akairo-master", "collection"
    )

    override fun execute(message: Message, args: Arguments): Response {
        val user = message.author

        if (!args.hasNext()) {
            val description = "You can find the Discord.JS Documentation at [discord.js.org](https://discord.js.org/)"
            return InfoResponse(user.name, description)
        }

        var query = args.next("Unable to find query w/o version!")
        var version = "stable"

        if (args.hasNext()) {
            version = query.toLowerCase()

            if (!defaultSources.contains(version)) {
                val githubStr = "https://raw.githubusercontent.com/discordjs/discord.js/docs/$version.json"

                version = try {
                    Jsoup.connect(githubStr)
                        .ignoreContentType(true)
                        .execute()

                    query = args.next("Unable to find query w/ github version!")
                    githubStr
                } catch (ex: Exception) {
                    "stable"
                }
            } else {
                query = args.next("Unable to find query w/ source version!")
            }
        }

        query = query.replace("#", ".")
        val apiUrl = "https://djsdocs.sorta.moe/v2/embed?src=$version&q=$query"

        return try {
            val response: Connection.Response = Jsoup.connect(apiUrl)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .execute()

            val responseBody: String = response.body().replace("\"icon_url\":", "\"iconUrl\":")
            JsonSerial.mapper.readValue(responseBody, DocResponse::class.java)
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorResponse(user.name, "Unable to find `$query` in the DJS Documentation!")
        }
    }
}
