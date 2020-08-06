package net.sourcebot.module.documentation.commands

import com.google.gson.Gson
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import net.sourcebot.api.alert.InfoAlert
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.module.documentation.utility.DocAlert
import org.jsoup.Connection
import org.jsoup.Jsoup

class DJSCommand : RootCommand() {
    override val name: String = "djs"
    override val description: String = "Allows the user to query the Discord.JS Documentation."
    override var cleanupResponse: Boolean = false

    private val defaultSources: MutableList<String> = mutableListOf(
        "stable", "master", "rpc", "commando", "akairo", "akairo-master", "collection"
    )

    override fun execute(message: Message, args: Arguments): Alert {
        val user = message.author

        if (!args.hasNext()) {
            val description = "You can find the Discord.JS Documentation at [discord.js.org](https://discord.js.org/)"
            return InfoAlert(user.name, description)
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
            Gson().fromJson(responseBody, DocAlert::class.java)
        } catch (ex: Exception) {
            ex.printStackTrace()
            val errDesc = "Unable to find `$query` in the DJS Documentation!"
            ErrorAlert(user.name, errDesc)
        }
    }

}
