package net.sourcebot.module.documentation.dochandlers

import me.theforbiddenai.jenkinsparserkotlin.Jenkins
import me.theforbiddenai.jenkinsparserkotlin.entities.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.api.response.ErrorResponse
import net.sourcebot.api.response.Response
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
 * @property selectorCache The [SelectorModel.selectorCache] cache object
 * @property jenkins The [Jenkins] object which handles the searching aspect of this class
 * @property baseUrl The base url to the Jenkins JavaDocs site
 */
class JenkinsHandler(
    url: String,
    val iconUrl: String,
    val responseTitle: String
) {

    private val selectorCache = SelectorModel.selectorCache
    private val jenkins: Jenkins = Jenkins(url)
    private val baseUrl: String by lazy {
        url.substring(0, url.lastIndexOf("/") + 1).trim()
    }

    /**
     * Retrieves the [Response] which may be a Selection Menu, the [DocResponse] filled with information
     * from the given query, or an [ErrorResponse]
     *
     * @param cmdMessage The [Message] which invoked the command
     * @param query The [String] containing the information of which the user is looking to attempt to retrieve
     * @return The selection menu, an [ErrorResponse], or the [DocResponse] containing the information attempting to be
     * retrieved
     */
    fun retrieveResponse(cmdMessage: Message, query: String): Response {
        val user = cmdMessage.author

        try {
            val infoList: List<Information> = jenkins.search(query)

            if (infoList.isEmpty()) {
                return ErrorResponse(user.name, "Unable to find `$query` in the $responseTitle!")
            }

            val docResponse = DocResponse()
            docResponse.setAuthor(responseTitle, null, iconUrl)

            return if (infoList.size == 1) {
                createDocResponse(docResponse, infoList[0])
            } else {
                val docStorage = SelectorModel(null, cmdMessage, infoList, this)
                selectorCache.addSelector(user, docStorage)

                createSelectionResponse(docResponse, infoList)

            }

        } catch (ex: Exception) {
            //ex.printStackTrace() // This is for debug purposes when enabled
            val errDesc = "Unable to find `$query` in the $responseTitle!"
            return ErrorResponse(user.name, errDesc)
        }
    }

    /**
     * Creates the [DocResponse] containing the information being searched for
     *
     * @param docResponse The [DocResponse] the information is being added to
     * @param information The [Information] found from searching the Jenkins JavaDocs
     * @return The modified [DocResponse] containing the information
     * @throws Error If an object was added to the information list that isn't [ClassInformation], [MethodInformation],
     * [FieldInformation], or [EnumInformation]
     */
    fun createDocResponse(docResponse: DocResponse, information: Information): DocResponse {
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

            else -> {
                throw Error("Found an object that does not belong.")
            }


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
                } else {
                    convertedValue
                }

                docResponse.addField(key, fieldValue.truncate(500), false)
            }
        }

        return docResponse
    }

    /**
     * Creates the selection [Response] from the given [Information]
     *
     * @param docResponse The [DocResponse] the selection menu is being put into
     * @param infoList The [List] of [Information] being added to the selection menu
     * @return The modified [DocResponse] containing the selection menu
     */
    private fun createSelectionResponse(docResponse: DocResponse, infoList: List<Information>): DocResponse {
        docResponse.setTitle("Type the id of the option you would like to select in chat:")
        docResponse.setFooter("Type cancel to delete this message.")

        infoList.sortedByDescending { it.name }

        for (i in infoList.indices) {

            if (docResponse.descriptionBuilder.length >= 712) {
                val format = "\n\nIds not shown above: %d to %d"
                docResponse.appendDescription(String.format(format, i + 1, infoList.size))
                break
            }

            val info = infoList[i]

            val name = if (info is MethodInformation) {
                val className = info.classInfo.name.substringBefore("<")
                "${info.type} $className#${info.name}"
            } else {
                "${info.type} ${info.name}"
            }

            val optionText = MarkdownUtil.maskedLink(name, info.url)
            docResponse.appendDescription("\n\n**${i + 1}** - $optionText")

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
    private fun getInfoName(className: String, infoName: String): String {
        return "$className#${infoName.substringAfter("(" + 1)}"
            .replace("<.*?>+".toRegex(), "")
    }

    /**
     * Converts anchor elements to their markdown equivalent
     *
     * @param element The element which contains the anchor elements
     * @param url The url from the [Information] object
     * @return The raw html of the element, but does not contain the anchor elements, but rather their markdown
     * equivalent
     */
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
                        contains("../") || contains("#") -> {

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

    /**
     * Retrieves a class url by name
     *
     * @param className The name of the class the user is attempting to get the url from
     * @return The found url or null
     */
    private fun retrieveClassUrl(className: String): String? {
        val urlList = jenkins.classMap
            .filter { (_, classMapName) -> return@filter classMapName.equals(className, true) }
            .map { (classUrl, _) -> classUrl }

        return if (urlList.isEmpty()) null else urlList[0]
    }

}