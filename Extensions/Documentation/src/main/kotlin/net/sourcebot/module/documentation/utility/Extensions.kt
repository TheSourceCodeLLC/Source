package net.sourcebot.module.documentation.utility

import com.overzealous.remark.Remark
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.jsoup.nodes.Element
import java.util.regex.Matcher
import java.util.regex.Pattern

private val hyperlinkPattern: Pattern = Pattern.compile("\\[.*?]\\(.*?\\)")
private val remark: Remark = Remark()

/**
 * Converts raw html to markdown
 * @receiver [String]
 * @return The formatted [String]
 */
fun String.toMarkdown(): String {
    return remark.convertFragment(this)
        .replace("\\", "")
        .replace("%5C", "\\")
}

/**
 * Truncates a [String], and removes the remnants of a leftover hyperlink if the hyperlinks happens to be cut off
 *
 * @receiver [String]
 * @param limit The length limit before the [String] is truncated
 * @param ellipsis The ellipsis to add if the [String] is over the length limit
 * @return The truncated string
 */
fun String.truncate(limit: Int, ellipsis: String = "..."): String {
    if (this.length <= limit) return this

    var returnStr: String = this
    val sbReturn: StringBuilder = StringBuilder(returnStr)

    val matcher: Matcher = hyperlinkPattern.matcher(this)
    val hyperlinkMap: MutableMap<Int, Int> = mutableMapOf()

    while (matcher.find()) {
        val hyperlink: String = matcher.group(0)

        val beginning = returnStr.indexOf(hyperlink)
        val end = beginning + hyperlink.length
        hyperlinkMap[beginning] = end

        // This is to preserve the indices of hyperlinks
        returnStr = returnStr.replaceFirst(hyperlink, " ".repeat(hyperlink.length))
    }

    sbReturn.setLength(limit - ellipsis.length)
    sbReturn.append(ellipsis)

    returnStr = sbReturn.toString()
    hyperlinkMap.forEach { (beginning, end) ->
        if (limit in (beginning + 1) until end) {
            returnStr = returnStr.replaceRange(beginning, limit, "").trim() + ellipsis
        }
    }

    return returnStr
}

/**
 * Appends to a [StringBuilder] only if it is not over the length limit
 *
 * @receiver [StringBuilder]
 * @param str The [String] to append to the [StringBuilder]
 * @param limit The length limit
 * @param ellipsis The ellipsis to add if the [String] is over the length limit after it adds the [str]
 * @return The [StringBuilder]
 */
fun StringBuilder.appendIfRoom(str: String, limit: Int, ellipsis: String = "...") {
    if (this.length >= limit) return

    this.append(str)
    if (this.length >= limit) {
        this.append(ellipsis)
    }
}

/**
 * Capitalizes all words in a [String]
 *
 * @receiver [String]
 * @return A [String] in which all of the words have been capitalized
 */
fun String.capitalizeAllWords(): String {
    return split(" ")
        .joinToString(" ") { it.toLowerCase().capitalize() }
}

/**
 * Adds a [MessageEmbed.Field] to an [EmbedBuilder] if the [fieldDescription] is not empty
 *
 * @receiver [EmbedBuilder]
 * @param fieldName The name of the field
 * @param fieldDescription The description of the field
 * @return The potentially modified [EmbedBuilder]
 */
fun EmbedBuilder.attemptAddEmbedField(fieldName: String, fieldDescription: String): EmbedBuilder {
    if (fieldDescription.isNotEmpty()) {
        this.addField(fieldName, fieldDescription, false)
    }

    return this
}

/**
 * Converts anchor elements to their markdown equivalent
 *
 * @receiver [Element]
 * @param baseUrl The url to the documentation
 * @return Raw html where the anchor elements have been replaced with their markdown equivalent
 */
fun Element.anchorsToHyperlinks(baseUrl: String): String {
    var html = this.html()
    val documentUrl = this.ownerDocument()?.baseUri()?.removeSuffix("/") ?: this.baseUri().removeSuffix("/")
    val isDocUrlBlank = documentUrl.isBlank()

    val baseClassUrl = if (isDocUrlBlank) {
        baseUrl.substringBeforeLast("#")
    } else {
        documentUrl
    }

    this.select("a").forEach {
        val href = it.attr("href")

        val createdUrl = with(href) {
            when {
                contains("https") -> href
                contains("#") && !contains("/") -> baseClassUrl + href
                contains("../") -> {
                    var modifiedDocUrl = if (isDocUrlBlank) {
                        baseClassUrl.substringBeforeLast("/")
                    } else {
                        baseClassUrl
                    }
                    var modifiedHref = href

                    while (modifiedHref.contains("../")) {
                        modifiedDocUrl = modifiedDocUrl.substringBeforeLast("/")
                        modifiedHref = modifiedHref.replaceFirst("../", "")
                    }

                    "$modifiedDocUrl/$modifiedHref"
                }
                contains(".html") -> {
                    val pkgUrl = baseUrl.substringBeforeLast("/")
                    "$pkgUrl/$this"
                }
                else -> "$baseUrl/${href.removePrefix("/")}"
            }
        }

        val sanitizedUrl = createdUrl.replace("(%5C)?(%29|\\))".toRegex(), "%5C)")
            .replace(" ", "%20")
        val text = it.html().toMarkdown()

        val isCodeBlock = it.parent().tagName()?.equals("code", true) ?: false
        val modifiedText = if (isCodeBlock && !text.contains("`")) "`$text`" else text

        val hyperlink = "[$modifiedText]($sanitizedUrl)"

        html = html.replace(it.outerHtml(), hyperlink)
    }

    return html
}