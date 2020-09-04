package net.sourcebot.module.documentation.commands

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.command.RootCommand
import net.sourcebot.api.command.argument.Arguments
import net.sourcebot.api.configuration.JsonSerial
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.InfoResponse
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.truncate
import net.sourcebot.module.documentation.utility.toMarkdown
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MDNCommand : RootCommand() {
    override val name: String = "mdn"
    override val description: String = "Allows the user to query the MDN Documentation."
    override var cleanupResponse: Boolean = false
    override val aliases: Array<String> = arrayOf("javascript", "js")

    private val baseUrl = "https://developer.mozilla.org"
    private val cache = MDNDocCache()

    override fun execute(message: Message, args: Arguments): Response {
        val user = message.author
        val iconUrl = "https://developer.mozilla.org/static/img/opengraph-logo.72382e605ce3.png"

        if (!args.hasNext()) {
            val description = "You can find the MDN Documentation at [developer.mozilla.org](https://developer.mozilla.org/en-US/docs/)"
            return InfoResponse(user.name, description)
        }

        val query = args.next("Unable to find query!").replace("#", ".").removeSuffix("()")

        if (cache.hasAlert(query)) {
            return cache.getAlert(query)!!
        }

        val connectionStr = "https://developer.mozilla.org/api/v1/search/en-US?q=$query"

        try {
            val searchDocument = Jsoup.connect(connectionStr)
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()

            val notFoundAlert = ErrorResponse(user.name, "Unable to find `$query` in the MDN Documentation!")


            val jsonObject = JsonSerial.mapper.readTree(searchDocument.body().text())
            val documentArray = jsonObject["documents"] as ArrayNode

            if (documentArray.size() == 0) {
                return notFoundAlert
            }

            val resultList: MutableList<JsonNode> = mutableListOf()

            documentArray.filter {
                val title = it["title"].asText()?.removeSuffix("()") ?: return@filter false
                return@filter title.equals(query, true)
            }.toCollection(resultList)

            if (resultList.isEmpty()) {
                val alertDescSB = StringBuilder()

                documentArray.forEach {
                    val title: String = it["title"].asText() ?: return@forEach
                    val url = "$baseUrl/${it["slug"].asText() ?: return@forEach}"
                    if (title.isEmpty()) return@forEach
                    val itemHyperlink = "[$title]($url)"
                    alertDescSB.append("**$itemHyperlink**\n")
                }

                if (alertDescSB.isEmpty()) return notFoundAlert

                val searchResultAlert = DocResponse()
                searchResultAlert.setAuthor("MDN Documentation", null, iconUrl)
                    .setTitle("Search Results:")
                    .setDescription(alertDescSB.toString())

                cache.putAlert(query, searchResultAlert)
                return searchResultAlert
            }

            val docObjectResult = resultList[0]
            val resultUrl = "$baseUrl/${docObjectResult["slug"].asText()}"

            val resultDocument = Jsoup.connect(resultUrl)
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()

            val docAlert = DocResponse()
            docAlert.setAuthor("MDN Documentation", null, iconUrl)

            val wikiElement = resultDocument.selectFirst("article#wikiArticle")
            wikiElement.html(wikiElement.html().replace("<p></p>", ""))

            val descriptionElement = wikiElement.selectFirst("article > p")
                    ?: return ErrorResponse(user.name, "Unable to find article description!")

            val description = hyperlinksToMarkdown(descriptionElement).toMarkdown().truncate(600)

            val anchorText = docObjectResult["title"].asText().replace(".", "#")

            val itemHyperlink = MarkdownUtil.maskedLink(anchorText, resultUrl)

            docAlert.setDescription("**__${itemHyperlink}__**\n$description")

            val propertyString = retrieveFormattedAnchorList(wikiElement, "Properties")
            if (propertyString.isNotEmpty()) docAlert.addField("Properties:", propertyString, false)

            val methodString = retrieveFormattedAnchorList(wikiElement, "Methods")
            if (methodString.isNotEmpty()) docAlert.addField("Methods:", methodString, false)

            val parameterString = retrieveFormattedElement(wikiElement, "Parameters")
            if (parameterString.isNotEmpty()) docAlert.addField("Parameters:", parameterString, false)

            val returnValString = retrieveFormattedElement(wikiElement, "Return_value")
            if (returnValString.isNotEmpty()) docAlert.addField("Returns:", returnValString, false)

            val exceptionString = retrieveFormattedElement(wikiElement, "Exceptions")
            if (exceptionString.isNotEmpty()) docAlert.addField("Exceptions:", exceptionString, false)

            val valueString = retrieveFormattedElement(wikiElement, "Value")
            if (valueString.isNotEmpty()) docAlert.addField("Value:", valueString, false)

            val throwString = retrieveFormattedElement(wikiElement, "Throws")
            if (throwString.isNotEmpty()) docAlert.addField("Throws:", throwString, false)

            cache.putAlert(query, docAlert)
            return docAlert
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ErrorResponse(user.name, "Something went wrong, please try again!")
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
            return hyperlinksToMarkdown(returnElement).toMarkdown()
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

                    val itemDesc = hyperlinksToMarkdown(descElement).toMarkdown().truncate(128)

                    val appendFormat = "$itemName - $itemDesc\n"
                    val appendString = if (count == 3 && descTagList.size > 4) "$appendFormat..." else "$appendFormat\n"

                    returnSB.append(appendString)
                    count++
                }

        returnSB.trimToSize()
        return returnSB.toString()

    }

    private fun hyperlinksToMarkdown(element: Element): String {
        var html = element.outerHtml()

        element.select("a").forEach {
            val url: String = MarkdownSanitizer.escape("$baseUrl${it.attr("href")}")
            val text: String = it.outerHtml().toMarkdown()
            val hyperlink: String = MarkdownUtil.maskedLink(text, url).replace("%29", ")")

            html = html.replace(it.outerHtml(), hyperlink)
        }

        return html
    }

    private class MDNDocCache {
        val mdnCache: MutableMap<String, Response> = mutableMapOf()

        fun hasAlert(query: String): Boolean {
            return mdnCache.containsKey(query)
        }

        fun putAlert(query: String, response: Response) {
            mdnCache[query.toLowerCase()] = response
        }

        fun getAlert(query: String): Response? {
            return mdnCache[query.toLowerCase()]
        }
    }
}