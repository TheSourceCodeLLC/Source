package net.sourcebot.module.documentation.commands

import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Group
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.DocumentationCommand
import net.sourcebot.module.documentation.utility.DocResponse
import org.jsoup.Connection
import org.jsoup.Jsoup

class DJSCommand : DocumentationCommand(
    "djs", "Allows the user to query the Discord.JS Documentation."
) {
    enum class Version(override val synopsisName: String) : Group.Option {
        STABLE("stable"),
        MASTER("master");

        companion object {
            @JvmStatic fun find(name: String): Version =
                values().first { name.equals(it.synopsisName, true) }
        }
    }

    override val synopsis = Synopsis {
        optGroup("version", "The version of D.JS you are seeking help for.", Version::find, Version.STABLE) {
            choice(Version.STABLE, "D.JS Stable")
            choice(Version.MASTER, "D.JS Master")
        }
        optParam("query", "The D.JS element you are seeking help for.", Adapter.single())
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val user = sender.author
        val version = arguments.optional("version", Version.STABLE)
        val query = arguments.optional<String>("query")
            ?.replace("#", ".")
        if (query == null) {
            val description = "You can find the Discord.JS Documentation at [discord.js.org](https://discord.js.org/)"
            return StandardInfoResponse(user.name, description)
        }
        val apiUrl = "https://djsdocs.sorta.moe/v2/embed?src=${version.synopsisName}&q=$query"

        return try {
            val response: Connection.Response = Jsoup.connect(apiUrl)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .execute()

            val responseBody: String = response.body().replace("\"icon_url\":", "\"iconUrl\":")
            JsonSerial.mapper.readValue(responseBody, DocResponse::class.java)
        } catch (ex: Exception) {
            //ex.printStackTrace()
            StandardErrorResponse(user.name, "Unable to find `$query` in the DJS Documentation!")
        }
    }
}
