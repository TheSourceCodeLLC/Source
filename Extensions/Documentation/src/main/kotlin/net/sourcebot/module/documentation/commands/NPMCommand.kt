package net.sourcebot.module.documentation.commands

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import me.hwiggy.kommander.arguments.Adapter
import me.hwiggy.kommander.arguments.Arguments
import me.hwiggy.kommander.arguments.Synopsis
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.ifPresentOrElse
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.DocumentationCommand

class NPMCommand : DocumentationCommand(
    "npm", "Allows you to query npmjs.org"
) {
    private val registry = "https://registry.npmjs.org/"
    override val synopsis = Synopsis {
        reqParam("package", "The package to query for.", Adapter.single())
        optParam("version", "The version of the package.", Adapter.single(), "latest")
    }

    override fun execute(sender: Message, arguments: Arguments.Processed): Response {
        val packageName = arguments.required<String>("package", "You did not specify a package to query for!")
        val version = arguments.optional("version", "latest")
        val query = runCatching {
            JsonSerial.fromUrl<ObjectNode>("$registry/$packageName")
        }.getOrNull() ?: return StandardErrorResponse(
            description = "I couldn't locate package `$packageName` in the NPM Registry!"
        )
        val versionInfo = if (version != "latest") {
            query["versions"][version] ?: return StandardErrorResponse(
                description = "I couldn't locate package `$packageName` version `$version` in the NPM Registry!"
            )
        } else query
        return versionInfo["time"]["unpublished"].ifPresentOrElse({
            PackageInfo(
                packageName,
                "N/A - Package was unpublished.",
                it["tags"]["latest"].asText(),
                it["maintainers"] as ArrayNode,
                true
            )
        }, {
            PackageInfo(
                packageName,
                versionInfo["description"].asText(),
                versionInfo["version"]?.asText() ?: query["dist-tags"]["latest"].asText(),
                versionInfo["maintainers"] as ArrayNode
            )
        }).render()
    }

    private class PackageInfo(
        private val name: String,
        private val description: String,
        private val version: String,
        private val maintainers: ArrayNode,
        private val unpublished: Boolean = false
    ) {
        val link = "https://npmjs.com/package/$name/v/$version"
        fun render() = StandardInfoResponse(
            "NPM Registry - $name", """
                **Package Name / URL:** ${
                if (unpublished) name else MarkdownUtil.maskedLink(name, link)
            }
                **Description:** $description
                **Version:** $version
                **Maintainers:** ${
                maintainers.map { it["name"].asText() }.joinToString(", ") { "`$it`" }
            } 
            """.trimIndent()
        )
    }
}