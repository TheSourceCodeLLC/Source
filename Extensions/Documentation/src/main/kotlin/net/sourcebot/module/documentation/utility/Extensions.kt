package net.sourcebot.module.documentation.utility

import com.overzealous.remark.Remark
import java.util.regex.Matcher
import java.util.regex.Pattern

private val hyperlinkPattern: Pattern = Pattern.compile("\\[.*?]\\(.*?\\)")
private val remark: Remark = Remark()

fun String.toMarkdown(): String {
    val hyperlinkRegex: Regex = "<code>\\[(.*?)]\\((.*?)\\)</code>".toRegex()
    val rawHtml = this.replace(hyperlinkRegex, "[<code>$1</code>]($2)")

    return remark.convertFragment(rawHtml).replace("\\", "")
}

// Truncates a string while preserving remaining hyperlinks
fun String.approxTruncate(approxLimit: Int): String {
    if (this.length <= approxLimit) return this

    var returnString: String = this
    val matcher: Matcher = hyperlinkPattern.matcher(this)

    val hyperlinkArray: ArrayList<String> = ArrayList()
    while (matcher.find()) {
        val hyperlink: String = matcher.group(0)

        returnString = returnString.replaceFirst(hyperlink, "$${hyperlinkArray.size}")
        hyperlinkArray.add(hyperlink)
    }

    val sbReturn: StringBuilder = StringBuilder(returnString)
    if (sbReturn.length > approxLimit) {
        sbReturn.setLength(approxLimit)
        sbReturn.append("...")
    }
    returnString = sbReturn.toString()

    if (hyperlinkArray.isNotEmpty()) {
        for (i in 0 until hyperlinkArray.size) {
            if (returnString.contains("$$i")) {
                returnString = returnString.replace("$$i", hyperlinkArray[i])
            } else {
                break
            }
        }
    }

    return returnString
}