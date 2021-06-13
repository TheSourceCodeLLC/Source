package net.sourcebot.module.documentation.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.api.response.StandardInfoResponse
import net.sourcebot.module.documentation.commands.bootstrap.DocumentationCommand
import net.sourcebot.module.documentation.utility.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.InputStreamReader
import java.net.URL

class MDNCommand : DocumentationCommand(
    "mdn", "Allows the user to query the MDN Documentation."
) {
    override val aliases: Array<String> = arrayOf("javascript", "js")
    private val baseUrl = "https://developer.mozilla.org"
    private val cache = MDNDocCache()

    override fun execute(message: Message, args: Arguments): Response {
        val user = message.author
        val iconUrl = "https://developer.mozilla.org/static/img/opengraph-logo.72382e605ce3.png"

        if (!args.hasNext()) {
            val description =
                "You can find the MDN Documentation at [developer.mozilla.org](https://developer.mozilla.org/en-US/docs/)"
            return StandardInfoResponse(user.name, description)
        }

        val query = args.next("Unable to find query!").replace("#", ".").removeSuffix("()")

        if (cache.hasResponse(query)) {
            return cache.getResponse(query)!!
        }

        val connectionStr = "https://developer.mozilla.org/api/v1/search/en-US?q=$query"

        try {
            val notFoundResponse = StandardErrorResponse(user.name, "Unable to find `$query` in the MDN Documentation!")
            val jsonObject = URL(connectionStr).openStream().use {
                val reader = InputStreamReader(it, "UTF-8")
                JsonSerial.mapper.readTree(reader)
            }

            val documentArray = jsonObject["documents"] as ArrayNode

            if (documentArray.isEmpty) {
                return notFoundResponse
            }

            val resultList: MutableList<JsonNode> = mutableListOf()

            documentArray.filter {
                val title = it["title"].asText()?.removeSuffix("()") ?: return@filter false
                val withoutPrototype = title.replace(".prototype", "")
                return@filter query.equals(title, true) || query.equals(withoutPrototype, true)
            }.toCollection(resultList)

            if (resultList.isEmpty()) {
                val responseDescSB = StringBuilder()

                documentArray.forEach {
                    val title: String = it["title"].asText() ?: return@forEach
                    val url = "$baseUrl/en-US/docs/${it["slug"].asText() ?: return@forEach}"
                    if (title.isEmpty()) return@forEach
                    val itemHyperlink = "[$title]($url)"
                    responseDescSB.append("**$itemHyperlink**\n")
                }

                if (responseDescSB.isEmpty()) return notFoundResponse

                val searchResultResponse = DocResponse()
                searchResultResponse.setAuthor("MDN Documentation", null, iconUrl)
                    .setTitle("Search Results:")
                    .setDescription(responseDescSB.toString())

                cache.putResponse(query, searchResultResponse)
                return searchResultResponse
            }


            val docObjectResult = resultList[0]
            val resultUrl = "$baseUrl/en-US/docs/${docObjectResult["slug"].asText()}"

            val resultDocument = URL(resultUrl).openStream().use {
                Jsoup.parse(it, "UTF-8", resultUrl)
            }

            val docResponse = DocResponse()
            docResponse.setAuthor("MDN Documentation", null, iconUrl)

            val wikiElement = resultDocument.selectFirst("article.main-page-content")
            wikiElement.html(wikiElement.html().replace("<p></p>", ""))

            val descriptionElement = wikiElement.selectFirst("div > p")
                ?: return StandardErrorResponse(user.name, "Unable to find article description!")

            val description = descriptionElement.anchorsToHyperlinks(baseUrl)
                .toMarkdown()
                .truncate(600)

            val anchorText = docObjectResult["title"].asText()

            val itemHyperlink = MarkdownUtil.maskedLink(anchorText, resultUrl)

            docResponse.setDescription("**__${itemHyperlink}__**\n$description")

            if (!anchorText.contains(".")) {
                val propertyString = retrieveFormattedSidebarList(resultDocument, "Properties")
                docResponse.attemptAddEmbedField("Properties:", propertyString)

                val methodString = retrieveFormattedSidebarList(resultDocument, "Methods")
                docResponse.attemptAddEmbedField("Methods:", methodString)
            }

            val headerNames = arrayOf("parameters", "return_value", "returns", "exceptions", "value", "throws")
            headerNames.forEach {
                val fieldName = if (it.equals("return_value", true)) "Returns:" else "${it.capitalize()}:"
                val fieldDescription = retrieveFormattedElement(resultDocument, it)

                docResponse.attemptAddEmbedField(fieldName, fieldDescription)
            }

            cache.putResponse(query, docResponse)
            return docResponse
        } catch (ex: Exception) {
            ex.printStackTrace()
            return StandardErrorResponse(user.name, "Something went wrong, please try again!")
        }

    }

    private fun retrieveFormattedSidebarList(document: Document, headerName: String): String {
        val sidebar = document.selectFirst("nav#sidebar-quicklinks > div > ol") ?: return ""

        val listHeader = sidebar.select("li").first { it.selectFirst("a")?.text().equals(headerName, true) }
        val listChildren = listHeader.selectFirst("ol")

        val builder = StringBuilder()
        listChildren.select("li").forEach {
            val anchor = it.selectFirst("a") ?: return@forEach
            val text = anchor.text()
            if (text.contains(" ") || text.contains("[")) return@forEach

            builder.append("`${text.substringAfterLast(".").removeSuffix("()")}` ")
        }

        return builder.toString().truncate(512).replace("\\s`[^`]*[.]{3}\$".toRegex(), "...")

    }

    private fun retrieveFormattedElement(document: Document, headerName: String): String {
        val header = document.selectFirst("h3#$headerName") ?: return ""

        val returnElement = header.nextElementSibling()
        val descTagList = returnElement.select("dt")

        if (descTagList.size == 0) {
            val desc = returnElement.selectFirst("p") ?: returnElement
            return desc.anchorsToHyperlinks(baseUrl).toMarkdown()
        }

        val returnSB = StringBuilder()
        descTagList.stream().limit(4)
            .forEach {
                // Prevents nested dl elements from showing up
                val parentOfParent = it.parent()?.parent()
                if (parentOfParent != null && parentOfParent.tagName().equals("dd", true)) return@forEach

                val itemName = it.html().toMarkdown().replace("Optional", "*")

                // Prevents text from a nested dl from being put into the item description
                var descElement = it.nextElementSibling() ?: return@forEach
                descElement = descElement.selectFirst("p") ?: descElement


                // Removes HTML list elements
                descElement.select("ul").remove()
                descElement.select("ol").remove()
                descElement.select("dl").remove()

                // Removes remnants of list elements (i.e. "This can either be:")
                descElement = descElement.html(descElement.html().replace("(\\.)(.*)[:]".toRegex(), "$1"))

                val itemDesc = descElement.anchorsToHyperlinks(baseUrl)
                    .toMarkdown()
                    .truncate(128)

                val appendFormat = "$itemName - $itemDesc\n"
                returnSB.append("$appendFormat\n")
            }

        returnSB.trimToSize()
        // Max field length is 1024, max itemDesc is 128, 1024-128-3(for truncation)=893
        return returnSB.toString().truncate(893)

    }

    private class MDNDocCache {
        val mdnCache: MutableMap<String, Response> = mutableMapOf()

        fun hasResponse(query: String): Boolean {
            return mdnCache.containsKey(query.toLowerCase())
        }

        fun putResponse(query: String, response: Response) {
            mdnCache[query.toLowerCase()] = response
        }

        fun getResponse(query: String): Response? {
            return mdnCache[query.toLowerCase()]
        }
    }
}