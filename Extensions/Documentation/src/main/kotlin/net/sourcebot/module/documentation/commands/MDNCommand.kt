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
import org.jsoup.nodes.Element

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
            val searchDocument = Jsoup.connect(connectionStr)
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()

            val notFoundResponse = StandardErrorResponse(user.name, "Unable to find `$query` in the MDN Documentation!")


            val jsonObject = JsonSerial.mapper.readTree(searchDocument.body().text())
            val documentArray = jsonObject["documents"] as ArrayNode

            if (documentArray.isEmpty) {
                return notFoundResponse
            }

            val resultList: MutableList<JsonNode> = mutableListOf()

            documentArray.filter {
                val title = it["title"].asText()
                    ?.replace(".prototype", "")
                    ?.removeSuffix("()") ?: return@filter false
                return@filter title.equals(query, true)
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

            val resultDocument = Jsoup.connect(resultUrl)
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()

            val docResponse = DocResponse()
            docResponse.setAuthor("MDN Documentation", null, iconUrl)

            val wikiElement = resultDocument.selectFirst("article.article")
            wikiElement.html(wikiElement.html().replace("<p></p>", ""))

            val descriptionElement = wikiElement.selectFirst("div > p")
                ?: return StandardErrorResponse(user.name, "Unable to find article description!")

            val description = descriptionElement.anchorsToHyperlinks(baseUrl)
                .toMarkdown()
                .truncate(600)

            val anchorText = docObjectResult["title"].asText().replace(".", "#")

            val itemHyperlink = MarkdownUtil.maskedLink(anchorText, resultUrl)

            docResponse.setDescription("**__${itemHyperlink}__**\n$description")

            val propertyString = retrieveFormattedAnchorList(wikiElement, "Properties")
            docResponse.attemptAddEmbedField("Properties:", propertyString)

            val methodString = retrieveFormattedAnchorList(wikiElement, "Methods")
            docResponse.attemptAddEmbedField("Methods:", methodString)

            val headerNames = arrayOf("Parameters", "Return_value", "Exceptions", "Value", "Throws")
            headerNames.forEach {
                val fieldName = if (it.equals("Return_value", true)) "Returns:" else "$it:"
                val fieldDescription = retrieveFormattedElement(wikiElement, it)

                docResponse.attemptAddEmbedField(fieldName, fieldDescription)
            }

            cache.putResponse(query, docResponse)
            return docResponse
        } catch (ex: Exception) {
            ex.printStackTrace()
            return StandardErrorResponse(user.name, "Something went wrong, please try again!")
        }

    }

    private fun retrieveFormattedAnchorList(wikiElement: Element, headerName: String): String {
        val elementLocator = wikiElement.selectFirst("h2#$headerName") ?: wikiElement.selectFirst("div#$headerName")
        ?: return ""

        var descListElement = if (elementLocator.tagName() == "div") {
            elementLocator.selectFirst("dl")
        } else {
            elementLocator.nextElementSibling()
        }

        if (descListElement.tagName() == "p") {
            descListElement = descListElement.nextElementSibling()
        }

        val returnSB = StringBuilder()
        val descTagList = descListElement.select("dt")

        descTagList.stream()
            .filter { it.selectFirst("a") != null && returnSB.length < 512 }
            .map { it.selectFirst("a") }
            .forEach {
                val text = it.text().substringAfter(".").removeSuffix("()")

                returnSB.append("`$text` ")
            }

        returnSB.trimToSize()
        return returnSB.toString()
    }

    private fun retrieveFormattedElement(wikiElement: Element, headerName: String): String {
        val header = wikiElement.selectFirst("h3#$headerName") ?: return ""

        val returnElement = header.nextElementSibling()
        val descTagList = returnElement.select("dt")

        if (descTagList.size == 0) {
            return returnElement.anchorsToHyperlinks(baseUrl)
                .toMarkdown()
        }

        val returnSB = StringBuilder()

        var count = 0
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
                val appendString = if (count == 3 && descTagList.size > 4) "$appendFormat..." else "$appendFormat\n"

                returnSB.append(appendString)
                count++
            }

        returnSB.trimToSize()
        return returnSB.toString()

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