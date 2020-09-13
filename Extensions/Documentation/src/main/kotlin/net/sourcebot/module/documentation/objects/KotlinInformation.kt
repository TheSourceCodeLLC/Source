package net.sourcebot.module.documentation.objects

import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.module.documentation.objects.impl.KotlinMember
import net.sourcebot.module.documentation.objects.impl.KotlinType
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.toMarkdown
import net.sourcebot.module.documentation.utility.truncate
import org.jsoup.nodes.Element

/**
 * Provides convenient access to retrieve the formatted description and tag lists from the Kotlin documentation,
 * along with providing away for converting anchor elements to markdown, for classes which inherit this abstract class
 *
 * @property iconUrl The response embed icon url
 * @property url The url to the specific [KotlinType]/[KotlinMember] on the kotlin documentation
 * @property type The type of the [KotlinType]/[KotlinMember] (i.e. Class, Extension Function, etc)
 * @property description The description from the kotlin documentation site for the specific KotlinType/KotlinMember
 * @property tags The tags from the kotlin docs for the specific [KotlinType]/[KotlinMember] (i.e. JS, JVM, Common, etc)
 */
abstract class KotlinInformation {

    internal val iconUrl = "https://pbs.twimg.com/profile_images/699217734492647428/pCfEzr6L_400x400.png"

    abstract val url: String
    abstract val name: String
    abstract val type: String
    abstract val description: String
    abstract val tags: ArrayList<String>

    /**
     * Generates the response embed for the [KotlinType]/[KotlinMember]
     *
     * @return The created response object
     */
    abstract fun createResponse(): DocResponse

    /**
     * Retrieves the type of this [KotlinInformation]
     *
     * @return The type string
     */
    internal abstract fun retrieveType(): String

    /**
     * Retrieves the description from the given element from the Kotlin Documentation site and converts it to markdown
     *
     * @param element The element from which to pull the description from
     * @return The formatted description
     */
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

    /**
     * Retrieves the tags from the given element from the Kotlin Documentation site
     *
     * @param element The element from which to retrieve the tag list from
     * @return An [ArrayList] of [String]s which contain the tag titles
     */
    internal fun retrieveTags(element: Element): ArrayList<String> {
        val tagArray = arrayListOf<String>()
        val tagDiv = element.selectFirst("div.tags")
        tagDiv.select("div")
            .map { it.text() }
            .filter { it?.isNotBlank() ?: return@filter false }
            .forEach { tagArray.add(it) }

        return tagArray
    }

    /**
     * Converts anchor elements to markdown
     *
     * @param element The element which contains the anchor elements to convert to hyperlinks
     * @return Raw html where the anchor elements have been replaced with their markdown equivalent
     */
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

            val sanitizedUrl = url.replace(")", "%5C)").replace(" ", "%20")
            val text: String = it.html().toMarkdown()
            val hyperlink = "[$text]($sanitizedUrl)"

            html = html.replace(it.outerHtml(), hyperlink)
        }

        return html
    }

}