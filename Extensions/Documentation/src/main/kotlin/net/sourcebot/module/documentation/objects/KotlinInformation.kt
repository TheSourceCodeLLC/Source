package net.sourcebot.module.documentation.objects

import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.response.Response
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.toMarkdown
import net.sourcebot.module.documentation.utility.truncate
import org.jsoup.nodes.Element

abstract class KotlinInformation {

    internal val iconUrl = "https://pbs.twimg.com/profile_images/699217734492647428/pCfEzr6L_400x400.png"

    abstract val url: String
    abstract val name: String
    abstract val type: String
    abstract val description: String
    abstract val tags: ArrayList<String>

    abstract fun createResponse(): DocResponse

    internal fun retrieveDescription(element: Element): String {
        var foundDescription: String
        if (element.hasClass("declarations")) {
            val divSummary = element.selectFirst("div.summary-group")
            val paragraphElement = divSummary.selectFirst("p")

            foundDescription = if (paragraphElement == null) {
                "N/A"
            } else {
                hyperlinksToMarkdown(paragraphElement).toMarkdown().truncate(500)
            }

            val signatureElement = divSummary.selectFirst("div.signature") ?: return description
            val codeBlock = MarkdownUtil.codeblock("kotlin", signatureElement.text())
            foundDescription += "\n$codeBlock"
        } else {
            val paragraphElement = element.selectFirst("p")
            foundDescription = hyperlinksToMarkdown(paragraphElement).toMarkdown().truncate(600)
        }

        return foundDescription
    }

    internal fun retrieveTags(element: Element): ArrayList<String> {
        val tagArray = arrayListOf<String>()
        val tagDiv = element.selectFirst("div.tags")
        tagDiv.select("div")
            .map { it.text() }
            .filter { it?.isNotBlank() ?: return@filter false }
            .forEach { tagArray.add(it) }

        return tagArray
    }

    internal fun hyperlinksToMarkdown(element: Element): String {
        var html = element.outerHtml()

        val baseUrl = "https://kotlinlang.org/api/latest/jvm/stdlib"
        val documentUrl = element.ownerDocument().baseUri().removeSuffix("/")

        element.select("a").forEach {
            val href = it.attr("href")

            val url = with(href) {
                when {
                    contains("https") -> href
                    contains("../") -> {
                        var modifiedDocUrl = documentUrl
                        var modifiedHref = href

                        while (modifiedHref.contains("../")) {
                            modifiedDocUrl = modifiedDocUrl.substringBeforeLast("/")
                            modifiedHref = modifiedHref.replaceFirst("../", "")
                        }

                        "$modifiedDocUrl/$modifiedHref"
                    }
                    else -> "$baseUrl/$href"
                }
            }

            val sanitizedUrl = MarkdownSanitizer.sanitize(url)
            val text: String = it.html().toMarkdown()
            val hyperlink: String = MarkdownUtil.maskedLink(text, sanitizedUrl).replace("%29", ")")

            html = html.replace(it.outerHtml(), hyperlink)
        }

        return html
    }

}