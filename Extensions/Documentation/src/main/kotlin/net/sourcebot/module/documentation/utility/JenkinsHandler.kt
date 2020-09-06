package net.sourcebot.module.documentation.utility

import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import me.theforbiddenai.jenkinsparserkotlin.entities.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
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

    fun retrieveDocAlert(cmdMessage: Message, user: User, query: String): Response {
        DocSelectorStorage.removeAndDeleteSelector(user)

        try {
            val infoList: List<Information> = jenkins.search(query)

            if (infoList.isEmpty()) {
                return ErrorResponse(user.name, "Unable to find `$query` in the $embedTitle!")
            }

            val docAlert = DocResponse()
            docAlert.setAuthor(embedTitle, null, iconUrl)

            return if (infoList.size == 1) {
                createDocumentationEmbed(docAlert, infoList[0])
            } else {
                val docStorage = DocSelectorStorage(null, cmdMessage, infoList, this)
                DocSelectorStorage.addSelector(user, docStorage)

                createSelectionEmbed(docAlert, infoList)

            }

        } catch (ex: Exception) {
            ex.printStackTrace() //- This is for debug purposes when enabled
            val errDesc = "Unable to find `$query` in the $embedTitle!"
            return ErrorResponse(user.name, errDesc)
        }
    }

    fun createDocumentationEmbed(docAlert: DocResponse, information: Information): DocResponse {
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
        val infoDescription: String = convertHyperlinksToMarkdown(infoDescElement, infoUrl)
            .toMarkdown()
            .truncate(600)

        val infoHyperlink: String = MarkdownUtil.maskedLink(infoName, infoUrl)
        docAlert.setDescription("**__${infoHyperlink}__**\n$infoDescription")

        if (information !is ClassInformation) {
            val rawExtraInfo: Map<String, String> = information.rawExtraInformation

            rawExtraInfo.forEach { (key, value) ->
                val modifiedValue = value.replace("\n", "<br>")


                val valueElement: Element = Jsoup.parse("<div>$modifiedValue</div>").selectFirst("div")
                val convertedValue = convertHyperlinksToMarkdown(valueElement, infoUrl)
                    .toMarkdown()
                    .replace("<\\[`?([^`]+)`?]\\((.*)\\)>".toRegex(), "[`<$1>`]($2)")

                val fieldValue = if (key.equals("Parameters:", true)) {
                    convertedValue.truncate(250)
                } else {
                    if (convertedValue.length > 500) {
                        convertedValue.truncate(500)
                    }
                    convertedValue
                }

                docAlert.addField(key, fieldValue, false)
            }
        }

        return docAlert
    }

    private fun createSelectionEmbed(docAlert: DocResponse, infoList: List<Information>): DocResponse {
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

            var name = "${info.type} ${info.name}"
            if (info is MethodInformation) {
                val className = info.classInfo.name.substringBefore("<")
                name = "${info.type} $className#${info.name}"
            }

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
            .forEach {
                val builderLength = strBuilder.length + it.length

                val itemName = it.substringBefore("<")
                val appendStr = if (builderLength >= 512) "`$itemName`..." else "`$itemName` "
                strBuilder.append(appendStr)
            }

        return strBuilder.toString().trim()
    }

    private fun getInfoName(className: String, infoName: String): String {
        return "$className#${infoName.substringAfter("(" + 1)}"
            .replace("<.*?>+".toRegex(), "")
    }

    private fun convertHyperlinksToMarkdown(element: Element, url: String): String {
        var html: String = element.outerHtml()

        element.select("a").stream()
            .filter { it.attr("href") != null }
            .forEach {

                val baseClassUrl: String = url.substringBeforeLast("#")
                var hrefUrl: String = it.attr("href")
                val text: String = it.html().toMarkdown()

                hrefUrl = with(hrefUrl) {
                    when {
                        contains("http") -> hrefUrl
                        contains(".html") -> {
                            val pkgUrl = url.substringBeforeLast("/")
                            "$pkgUrl/$this"
                        }
                        contains("../") || contains("#") && !contains(".html") -> {

                            if (contains("#") && !contains("/")) {
                                return@with baseClassUrl + url
                            }

                            val modifiedHref = replace("../", "")

                            if (it.hasAttr("title")) {
                                val newPackage = it.attr("title")
                                    .replace("class in ", "")
                                    .replace(".", "/")
                                    .trim()

                                val parentPkgDir = "/${newPackage.substringBefore("/")}/"
                                val basePkgUrl = url.substringBeforeLast(parentPkgDir) + parentPkgDir

                                basePkgUrl + modifiedHref
                            } else {
                                baseUrl + modifiedHref
                            }

                        }
                        contains(".html", true) -> {
                            val modifiedHref = substring(lastIndexOf("/") + 1)
                                .substringBeforeLast(".")

                            retrieveClassUrl(modifiedHref)?.trim() ?: return@forEach
                        }
                        else -> this
                    }

                }

                if (hrefUrl.isNotEmpty()) {
                    val isCodeBlock = it.parent().tagName()?.equals("code", true) ?: false

                    val modifiedText = if (isCodeBlock && !text.contains("`")) "`$text`" else text

                    hrefUrl = MarkdownSanitizer.escape(hrefUrl)
                    val hyperlink: String = MarkdownUtil.maskedLink(modifiedText, hrefUrl)
                        .replace("%29", ")")

                    html = html.replace(it.outerHtml(), hyperlink)
                }

            }


        // Removes code tags that surround a hyperlink
        return html.replace("<code>(\\[.*?\\).*?)</code>".toRegex(), "$1")
    }


    private fun retrieveClassUrl(className: String): String? {
        val urlList = jenkins.classList.stream()
            .filter {
                val modifiedElement = it.substring(it.lastIndexOf("/") + 1).removeSuffix(".html")
                return@filter modifiedElement.equals(className, true)
            }.collect(Collectors.toList())

        return if (urlList.size == 0) null else urlList[0]
    }
}