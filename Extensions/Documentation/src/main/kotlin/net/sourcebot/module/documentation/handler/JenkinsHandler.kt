package net.sourcebot.module.documentation.handler

import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import me.theforbiddenai.jenkinsparserkotlin.entities.*
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.formatted
import net.sourcebot.api.menus.MenuHandler
import net.sourcebot.api.response.Response
import net.sourcebot.api.response.StandardErrorResponse
import net.sourcebot.module.documentation.utility.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.stream.Collectors

/**
 * This class handles the searching of all Jenkins JavaDocs and the creation of the [DocResponse]s
 *
 * @param url The url to the Jenkins JavaDocs tree or allclasses page
 * @property iconUrl The icon url for the [DocResponse]
 * @property responseTitle The title for the [DocResponse]
 * @property jenkins The [Jenkins] object which handles the searching aspect of this class
 * @property baseUrl The base url to the Jenkins JavaDocs site
 */
class JenkinsHandler(
    url: String,
    private val iconUrl: String,
    private val responseTitle: String
) {
    private val jenkins: Jenkins = Jenkins(url)
    private val baseUrl: String by lazy {
        url.substring(0, url.lastIndexOf("/") + 1).trim()
    }

    /**
     * Retrieves the [Response] which may be a Selection Menu, the [DocResponse] filled with information
     * from the given query, or an [StandardErrorResponse]
     *
     * @param user The [User] the doc response should be built for
     * @param query The [String] containing the information of which the user is looking to attempt to retrieve
     * @return The selection menu, an [StandardErrorResponse], or the [DocResponse] containing the information attempting to be
     * retrieved
     */
    fun retrieveResponse(user: User, query: String): Response {
        val error = StandardErrorResponse(user.formatted(), "Unable to find `$query` in the $responseTitle!")
        try {
            val infoList = jenkins.search(query)
            if (infoList.isEmpty()) return error
            return if (infoList.size == 1)
                createDocResponse(infoList[0])
            else MenuHandler.createSelectionMenu(
                infoList, 5, { info ->
                    val name = "${info.type} " + if (info is MethodInformation) {
                        val className = info.classInfo.name.substringBefore("<")
                        "$className#${info.name}"
                    } else info.name
                    val link = MarkdownUtil.maskedLink(name, info.url)
                    if (link.length > 200) name else link
                }, this::createDocResponse
            ).render().apply { setAuthor(responseTitle, null, iconUrl) }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return error
        }
    }

    /**
     * Creates the [DocResponse] containing the information being searched for
     *
     * @param information The [Information] found from searching the Jenkins JavaDocs
     * @return The modified [DocResponse] containing the information
     * @throws Error If an object was added to the information list that isn't [ClassInformation], [MethodInformation],
     * [FieldInformation], or [EnumInformation]
     */
    private fun createDocResponse(information: Information): DocResponse {
        val docResponse = DocResponse()
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

                docResponse.attemptAddEmbedField("Nested Classes:", formatList(nestedClassList))
                docResponse.attemptAddEmbedField("Methods:", formatList(methodList))
                docResponse.attemptAddEmbedField("Enums:", formatList(information.enumList))
                docResponse.attemptAddEmbedField("Fields:", formatList(information.fieldList))
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

            else -> throw Error("Found an object that does not belong.")
        }

        val infoDescElement: Element = Jsoup.parse("<div>$infoRawDescription</div>").selectFirst("div")
        val infoDescription: String = convertHyperlinksToMarkdown(infoDescElement, infoUrl)
            .toMarkdown()
            .truncate(600)

        val infoHyperlink: String = MarkdownUtil.maskedLink(infoName, infoUrl)
        docResponse.setDescription("**__${infoHyperlink}__**\n$infoDescription")

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
                } else convertedValue

                docResponse.addField(key, fieldValue.truncate(500), false)
            }
        }

        return docResponse
    }

    /**
     * Converts a [List] of [String] to a [String] with the format of `item1`, `item2`
     *
     * @param list The [List] being formatted
     * @param limit The length limit of the [String]
     * @return The [String] made from the converted [List]
     */
    private fun formatList(list: List<String>, limit: Int = 512): String {
        val strBuilder = StringBuilder()

        list.stream()
            .filter { !strBuilder.toString().contains(it, true) && strBuilder.length <= 512 }
            .forEach {
                val builderLength = strBuilder.length + it.length

                val itemName = it.substringBefore("<")
                val appendStr = if (builderLength >= limit) "`$itemName`..." else "`$itemName` "
                strBuilder.append(appendStr)
            }

        return strBuilder.toString().trim()
    }

    /**
     * Gets the info name and removes anything inside parenthesis or angled brackets
     *
     * @param className The name of the class
     * @param infoName The name of the info
     * @return The combined name
     */
    private fun getInfoName(
        className: String,
        infoName: String
    ) = "$className#${infoName.substringAfter("(" + 1)}"
        .replace("<.*?>+".toRegex(), "")

    /**
     * Converts anchor elements to their markdown equivalent
     *
     * @param element The element which contains the anchor elements
     * @param url The url from the [Information] object
     * @return The raw html of the element, but does not contain the anchor elements, but rather their markdown
     * equivalent
     */
    private fun convertHyperlinksToMarkdown(
        element: Element, url: String
    ) = element.anchorsToHyperlinks(url)
        // Removes code tags that surround a hyperlink
        .replace("<code>(\\[.*?\\).*?)</code>".toRegex(), "$1")
}