package net.sourcebot.module.documentation.objects.impl

import net.dv8tion.jda.api.utils.MarkdownUtil
import net.sourcebot.module.documentation.objects.KotlinInformation
import net.sourcebot.module.documentation.utility.DocResponse
import net.sourcebot.module.documentation.utility.capitalizeAllWords
import org.jsoup.nodes.Element

/**
 * This class contains information of the members of a [KotlinType], an example of a [KotlinMember] is a property
 * and an extension function
 *
 * @property memberElement The element from which the information is pulled from
 * @property parentName The name of the [KotlinType] the the [KotlinMember] belongs to
 */
class KotlinMember(
    private val memberElement: Element,
    private val parentName: String
) : KotlinInformation() {

    override lateinit var url: String
    override lateinit var name: String
    override val type: String by lazy { retrieveType() }
    override val description: String by lazy { retrieveDescription(memberElement) }
    override val tags: ArrayList<String> by lazy { retrieveTags(memberElement) }

    /**
     * Initializes the name and url properties
     */
    init {
        val header = memberElement.selectFirst("h4")
        val anchor = header.selectFirst("a")
        val documentUri = memberElement.ownerDocument().baseUri()

        name = header.text()
        url = documentUri + anchor.attr("href")
    }

    /**
     * @see KotlinInformation for information about this function
     */
    override fun createResponse(): DocResponse {
        val hyperlink = MarkdownUtil.maskedLink("$parentName#$name", url)
        val docResponse = DocResponse()

        docResponse.setAuthor("Kotlin Documentation", null, iconUrl)
        docResponse.setDescription("**__${hyperlink}__**\n$description")

        return docResponse
    }

    /**
     * @see KotlinInformation for information about this function
     */
    override fun retrieveType(): String {
        val parentDiv = memberElement.parent()
        val headerName = parentDiv.previousElementSibling().text()
        val pluralType = headerName.capitalizeAllWords()

        return if (pluralType.endsWith("ies")) {
            pluralType.replace("ies", "y")
        } else {
            pluralType.removeSuffix("s")
        }
    }

}