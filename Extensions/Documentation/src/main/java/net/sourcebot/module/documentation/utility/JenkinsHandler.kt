package net.sourcebot.module.documentation.utility

import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import me.theforbiddenai.jenkinsparserkotlin.entities.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.alert.Alert
import net.sourcebot.api.alert.ErrorAlert
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.reflect.Field
import java.util.stream.Collectors

class JenkinsHandler(
    url: String,
    val iconUrl: String,
    val embedTitle: String
) {

    private val jenkins: Jenkins = Jenkins(url)
    private val baseUrl: String by lazy {
        url.substring(0, url.lastIndexOf("/") + 1).trim()
    }
    private val classURLList: List<String> by lazy {
        val field: Field = Jenkins::class.java.getDeclaredField("classList")
        field.trySetAccessible()
        return@lazy field.get(jenkins) as List<String>
    }

    fun retrieveDocAlert(cmdMessage: Message, user: User, query: String): Alert {
        DocSelectorStorage.removeAndDeleteSelector(user)

        try {
            val infoList: List<Information> = jenkins.search(query)

            // return unable to find embed
            if (infoList.isEmpty()) return ErrorAlert(user.name, "Unable to find `$query` in the $embedTitle!")

            val docAlert = DocAlert()
            docAlert.setAuthor(embedTitle, null, iconUrl)

            if (infoList.size == 1) {
                return createDocumentationEmbed(docAlert, infoList[0])
            } else {
                val docStorage = DocSelectorStorage(null, cmdMessage, infoList, this)
                DocSelectorStorage.addSelector(user, docStorage)

                return createSelectionEmbed(docAlert, infoList)
            }

        } catch (ex: Exception) {
            val errDesc = "Unable to find `$query` in the $embedTitle!"
            return ErrorAlert(user.name, errDesc)
        }
    }

    fun createDocumentationEmbed(docAlert: DocAlert, information: Information): DocAlert {
        var infoName: String = MarkdownSanitizer.sanitize(information.name)
        val infoRawDescription: String = information.rawDescription
        val infoUrl: String = MarkdownSanitizer.sanitize(information.url)

        when (information) {
            is ClassInformation -> {

                val nestedClassList: List<String> = information.nestedClassList.stream()
                    .map { it.replace("$infoName.", "") }
                    .collect(Collectors.toList())

                val methodList: List<String> = information.methodList.stream()
                    .map { it.substringBefore("(").trim() }
                    .collect(Collectors.toList())

                docAlert.attemptAddEmbedField(nestedClassList, "Nested Classes:")
                docAlert.attemptAddEmbedField(methodList, "Methods:")
                docAlert.attemptAddEmbedField(information.enumList, "Enums:")
                docAlert.attemptAddEmbedField(information.fieldList, "Fields:")
            }

            is MethodInformation -> {
                val methodName = information.name.substringBefore("(").trim()
                infoName = getInfoName(information.classInfo.name, methodName)
            }

            is FieldInformation -> {
                infoName = getInfoName(information.classInfo.name, information.name)
            }

            is EnumInformation -> {
                infoName = getInfoName(information.classInfo.name, information.name)
            }

            else -> {
                throw Error("Found an object that does not belong.")
            }


        }

        val infoDescElement: Element = Jsoup.parse("<div>$infoRawDescription</div>").selectFirst("div")
        var infoDescription: String = convertHyperlinksToMarkdown(infoDescElement, infoUrl).toMarkdown()
        infoDescription = infoDescription.approxTruncate(600)

        val infoHyperlink: String = MarkdownUtil.maskedLink(infoName, infoUrl)
        docAlert.setDescription("**__${infoHyperlink}__**\n$infoDescription")

        if (information !is ClassInformation) {
            val rawExtraInfo: Map<String, String> = information.rawExtraInformation

            rawExtraInfo.forEach { (key, value) ->
                val replacementNewline = if (key.equals("Parameters:", true)) "<br><br>" else "<br>"

                val modifiedValue = value.replace("\n", replacementNewline)

                val valueElement: Element = Jsoup.parse("<div>$modifiedValue</div>").selectFirst("div")
                val convertedValue = convertHyperlinksToMarkdown(valueElement, infoUrl).toMarkdown()


                docAlert.addField(key, convertedValue, false)
            }
        }

        return docAlert
    }

    private fun createSelectionEmbed(docAlert: DocAlert, infoList: List<Information>): DocAlert {
        docAlert.setTitle("Type the id of the option you would like to select in chat:")
        docAlert.setFooter("Type cancel to delete this message.")

        infoList.sortedByDescending { it.name }

        for (i in infoList.indices) {

            if (docAlert.descriptionBuilder.length >= 712) {
                val format = "\n\nIds not shown above: %d to %d"
                docAlert.appendDescription(String.format(format, i + 1, infoList.size))
                break
            }

            val info = infoList[i]
            val name = "${info.type} ${info.name}"
            val optionText = MarkdownUtil.maskedLink(name, info.url)

            docAlert.appendDescription("\n\n**${i + 1}** - $optionText")

        }

        return docAlert
    }

    private fun EmbedBuilder.attemptAddEmbedField(fieldList: List<String>, fieldName: String): EmbedBuilder {
        if (fieldList.isNotEmpty()) {
            this.addField(fieldName, formatList(fieldList), false)
        }

        return this
    }

    private fun formatList(list: List<String>): String {
        val strBuilder = StringBuilder()

        list.stream()
            .filter { !strBuilder.toString().contains(it, true) && strBuilder.length <= 512 }
            .forEach { strBuilder.append("`$it` ") }

        if (strBuilder.length >= 512) {
            strBuilder.append("...")
        }

        return strBuilder.toString().trim()
    }

    private fun getInfoName(className: String, infoName: String): String {
        return "$className#${infoName.substringAfter("(" + 1)}"
    }

    private fun convertHyperlinksToMarkdown(element: Element, url: String): String {
        var html: String = element.outerHtml()

        element.select("a").stream()
            .filter { it.attr("href") != null }
            .forEach {

                var hrefUrl: String = it.attr("href")
                val text: String = it.outerHtml().toMarkdown()

                if (!hrefUrl.contains("http", true)) {
                    if (hrefUrl.contains("../") || hrefUrl.contains("#")) {
                        hrefUrl = hrefUrl.replace("../", "")
                        val baseClassUrl = url.substringBeforeLast("#")

                        hrefUrl = if (hrefUrl.contains("#") && !hrefUrl.contains("/")) {
                            baseClassUrl + hrefUrl
                        } else baseUrl + hrefUrl

                    } else if (hrefUrl.contains(".html", true)) {
                        hrefUrl = hrefUrl.substring(hrefUrl.lastIndexOf("/") + 1).substringBeforeLast(".")
                        hrefUrl = retrieveClassUrl(hrefUrl)?.trim() ?: return@forEach
                    }

                }

                if (hrefUrl.isNotEmpty()) {
                    hrefUrl = MarkdownSanitizer.escape(hrefUrl)
                    val hyperlink: String = MarkdownUtil.maskedLink(text, hrefUrl).replace("%29", ")")

                    html = html.replace(it.outerHtml(), hyperlink)
                }

            }

        html = html.replace("<code>\\[(.*?)]\\((.*?)\\)</code>".toRegex(), "[`$1`]($2)")

        // This prevents an issue, if there are 2 or more hyperlinks in the same code block
        return html.replace("<code>(\\[.*?\\).*?)</code>".toRegex(), "$1")
    }


    private fun retrieveClassUrl(className: String): String? {
        val urlList = classURLList.stream()
            .filter {
                val modifiedElement = it.substring(it.lastIndexOf("/") + 1).removeSuffix(".html")
                return@filter modifiedElement.equals(className, true)
            }.collect(Collectors.toList())

        return if (urlList.size == 0) null else urlList[0]
    }
}