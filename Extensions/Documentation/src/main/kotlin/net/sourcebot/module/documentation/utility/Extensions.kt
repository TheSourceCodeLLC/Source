package net.sourcebot.module.documentation.utility

import com.overzealous.remark.Remark
import java.util.regex.Matcher
import java.util.regex.Pattern

private val hyperlinkPattern: Pattern = Pattern.compile("\\[.*?]\\(.*?\\)")
private val remark: Remark = Remark()

fun String.toMarkdown(): String {
    return remark.convertFragment(this).replace("\\", "")
}

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